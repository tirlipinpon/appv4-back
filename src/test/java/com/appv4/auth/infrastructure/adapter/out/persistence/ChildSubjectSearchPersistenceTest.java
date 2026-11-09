package com.appv4.auth.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Verifies bounded child subject search on PostgreSQL. */
@DataJpaTest(showSql = false, properties = {"spring.jpa.hibernate.ddl-auto=validate",
		"spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("persistence-test")
@Import(ChildSubjectPersistenceAdapter.class)
@EnabledIfEnvironmentVariable(named = "APPV4_PERSISTENCE_TEST_URL", matches = ".+")
class ChildSubjectSearchPersistenceTest {

	@Autowired
	private ChildSubjectPersistenceAdapter adapter;
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private EntityManager entityManager;

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> System.getenv("APPV4_PERSISTENCE_TEST_URL"));
		registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_USER", "appv4"));
		registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_PASSWORD", "appv4"));
	}

	@Test
	void searchPagesDoNotLoadTheWholeCatalogueAndRespectExclusions() {
		long user = jdbc.queryForObject("insert into back_users (email, display_name) values (?, 'Test') returning id",
				Long.class, UUID.randomUUID() + "@example.test");
		long child = jdbc.queryForObject("""
				insert into back_children (parent_user_id, first_name, last_name, age, curriculum,
				grade_label, school_id, avatar_code, pin_hash)
				values (?, 'Test', 'Child', 8, 'FRANCE', 'CE1', 1, 'fox', 'hash') returning id
				""", Long.class, user);
		String prefix = UUID.randomUUID().toString();
		jdbc.update("""
				insert into back_teacher_subjects (user_id, school_id, grade_label, name, name_key)
				select ?, 1, 'CE2', 'Course ' || n, ? || n from generate_series(1, 1000) n
				""", user, prefix);
		var ids = jdbc.queryForList("select id from back_teacher_subjects where user_id = ? order by id", Long.class, user);
		jdbc.update("insert into back_child_subject_links (child_id, subject_id, excluded) values (?, ?, false)",
				child, ids.get(0));
		jdbc.update("insert into back_child_subject_links (child_id, subject_id, excluded) values (?, ?, true)",
				child, ids.get(1));
		assertThat(adapter.visibleLinksForParent(user)).hasSize(1);
		adapter.save(child, ids.get(1), false);
		assertThat(adapter.visibleLinksForParent(user)).hasSize(2);
		adapter.save(child, ids.get(1), true);
		assertThat(adapter.visibleLinksForParent(user)).hasSize(1);
		long classDefault = jdbc.queryForObject("""
				insert into back_teacher_subjects (user_id, school_id, grade_label, name, name_key)
				values (?, 1, 'CE1', 'Class default', ?) returning id
				""", Long.class, user, prefix + "class-default");
		long classExcluded = jdbc.queryForObject("""
				insert into back_teacher_subjects (user_id, school_id, grade_label, name, name_key)
				values (?, 1, 'CE1', 'Class excluded', ?) returning id
				""", Long.class, user, prefix + "class-excluded");
		jdbc.update("insert into back_child_subject_links (child_id, subject_id, excluded) values (?, ?, true)",
				child, classExcluded);
		jdbc.update("""
				insert into back_teacher_subjects (user_id, school_id, grade_label, name, name_key, hidden, active)
				values (?, 1, 'CE2', 'Blocked hidden', ?, true, true),
				       (?, 1, 'CE2', 'Blocked inactive', ?, false, false)
				""", user, prefix + "hidden", user, prefix + "inactive");
		entityManager.flush();
		entityManager.clear();
		var stats = entityManager.getEntityManagerFactory().unwrap(org.hibernate.SessionFactory.class).getStatistics();
		stats.clear();
		var first = adapter.searchOffers(child, "course", 1, "CE2", 1, "CE1", 0, 41);
		assertThat(first).hasSize(41);
		assertThat(first).extracting(item -> item.id()).contains(ids.get(1)).doesNotContain(ids.get(0)).isSorted();
		assertThat(stats.getEntityLoadCount()).isLessThan(100);
		var next = adapter.searchOffers(child, "course", 1, "CE2", 1, "CE1", first.get(40).id(), 41);
		assertThat(next).hasSize(41);
		assertThat(next.get(0).id()).isGreaterThan(first.get(40).id());
		assertThat(adapter.searchOffers(child, "course", 1, "CE1", 1, "CE1", 0, 41)).isEmpty();
		assertThat(adapter.searchOffers(child, "course", 2, "CE2", 1, "CE1", 0, 41)).isEmpty();
		assertThat(adapter.searchOffers(child, "blocked", 0, "", 1, "CE1", 0, 41)).isEmpty();
		assertThat(adapter.searchOffers(child, "class", 1, "CE1", 1, "CE1", 0, 41))
				.extracting(item -> item.id()).containsExactly(classExcluded).doesNotContain(classDefault);
	}
}
