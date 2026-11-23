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

/** Verifies batched subject listing on PostgreSQL. */
@DataJpaTest(showSql = false, properties = {"spring.jpa.hibernate.ddl-auto=validate",
		"spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("persistence-test")
@Import(TeacherSubjectPersistenceAdapter.class)
@EnabledIfEnvironmentVariable(named = "APPV4_PERSISTENCE_TEST_URL", matches = ".+")
class TeacherSubjectListingPersistenceTest {

	@Autowired
	private TeacherSubjectPersistenceAdapter adapter;
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
	void listingUsesBoundedQueriesAndKeepsAccessState() {
		long owner = user();
		long viewer = user();
		for (int count = 1; count <= 100; count++) {
			long subject = jdbc.queryForObject("""
					insert into back_teacher_subjects (user_id, school_id, grade_label, name, name_key)
					values (?, 1, 'CE1', ?, ?) returning id
					""", Long.class, owner, "Subject " + count, UUID.randomUUID().toString());
			jdbc.update("insert into back_teacher_subject_access (subject_id, user_id) values (?, ?)", subject, owner);
			if (count == 1) {
				jdbc.update("insert into back_teacher_subject_access (subject_id, user_id, active) values (?, ?, false)",
						subject, viewer);
			}
			if (count == 1 || count == 10 || count == 100) {
				entityManager.flush();
				entityManager.clear();
				var stats = entityManager.getEntityManagerFactory()
						.unwrap(org.hibernate.SessionFactory.class).getStatistics();
				stats.clear();
				var rows = adapter.findListing(viewer, java.util.List.of(1L), java.util.List.of("CE1"));
				assertThat(rows).hasSize(count);
				assertThat(stats.getPrepareStatementCount()).isEqualTo(4);
				assertThat(rows.get(0).followed()).isTrue();
				assertThat(rows.get(0).subject().active()).isFalse();
				assertThat(rows.get(0).others()).hasSize(1);
			}
		}
	}

	private long user() {
		return jdbc.queryForObject("insert into back_users (email, display_name) values (?, 'Test') returning id",
				Long.class, UUID.randomUUID() + "@example.test");
	}
}
