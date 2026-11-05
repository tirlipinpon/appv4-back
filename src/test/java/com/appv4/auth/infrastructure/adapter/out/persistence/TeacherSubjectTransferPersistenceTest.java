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

/** Verifies transfer persistence against a disposable PostgreSQL database. */
@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("persistence-test")
@Import(TeacherSubjectPersistenceAdapter.class)
@EnabledIfEnvironmentVariable(named = "APPV4_PERSISTENCE_TEST_URL", matches = ".+")
class TeacherSubjectTransferPersistenceTest {

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
	void transferPersistsOwnerAccessAndReactivationAfterClearingContext() {
		long owner = user();
		long recipient = user();
		long subject = jdbc.queryForObject("""
				insert into back_teacher_subjects (user_id, school_id, grade_label, name, name_key, active)
				values (?, 1, 'CE1', 'Test', ?, false) returning id
				""", Long.class, owner, UUID.randomUUID().toString());
		jdbc.update("insert into back_teacher_subject_access (subject_id, user_id) values (?, ?)", subject, owner);
		adapter.replaceAccessWith(subject, recipient);
		entityManager.flush();
		entityManager.clear();
		assertThat(jdbc.queryForObject("select user_id from back_teacher_subjects where id = ?", Long.class, subject))
				.isEqualTo(recipient);
		assertThat(jdbc.queryForObject("select active from back_teacher_subjects where id = ?", Boolean.class, subject))
				.isTrue();
		assertThat(jdbc.queryForList("select user_id from back_teacher_subject_access where subject_id = ?", Long.class, subject))
				.containsExactly(recipient);
	}

	private long user() {
		return jdbc.queryForObject("insert into back_users (email, display_name) values (?, 'Test') returning id",
				Long.class, UUID.randomUUID() + "@example.test");
	}
}
