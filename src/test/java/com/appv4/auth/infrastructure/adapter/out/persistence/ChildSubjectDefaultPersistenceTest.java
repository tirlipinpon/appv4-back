package com.appv4.auth.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** PostgreSQL test of concurrent default initialization. */
@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("persistence-test")
@Import(ChildSubjectPersistenceAdapter.class)
@EnabledIfEnvironmentVariable(named = "APPV4_PERSISTENCE_TEST_URL", matches = ".+")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ChildSubjectDefaultPersistenceTest {
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private ChildSubjectPersistenceAdapter adapter;

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> System.getenv("APPV4_PERSISTENCE_TEST_URL"));
		registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_USER", "appv4"));
		registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_PASSWORD", "appv4"));
	}

	@Test
	void concurrentInitializationCreatesOneLinkAndPreservesExclusion() throws Exception {
		long user = jdbc.queryForObject("insert into back_users (email, display_name) values (?, 'Test') returning id",
				Long.class, UUID.randomUUID() + "@example.test");
		try {
			long child = jdbc.queryForObject("""
					insert into back_children (parent_user_id, first_name, last_name, age, curriculum,
					grade_label, school_id, avatar_code, pin_hash)
					values (?, 'Test', 'Child', 8, 'FRANCE', 'CE1', 1, 'cat', 'hash') returning id
					""", Long.class, user);
			long subject = jdbc.queryForObject("""
					insert into back_teacher_subjects (user_id, school_id, grade_label, name, name_key)
					values (?, 1, 'CE1', 'Test', ?) returning id
					""", Long.class, user, UUID.randomUUID().toString());
			var start = new CountDownLatch(1);
			try (var pool = Executors.newFixedThreadPool(2)) {
				var first = pool.submit(() -> {
					start.await();
					adapter.ensureDefault(child, subject);
					return null;
				});
				var second = pool.submit(() -> {
					start.await();
					adapter.ensureDefault(child, subject);
					return null;
				});
				start.countDown();
				first.get(20, TimeUnit.SECONDS);
				second.get(20, TimeUnit.SECONDS);
			}
			assertThat(jdbc.queryForObject("select count(*) from back_child_subject_links "
					+ "where child_id = ? and subject_id = ?",
					Long.class, child, subject)).isEqualTo(1);
			adapter.save(child, subject, true);
			adapter.ensureDefault(child, subject);
			assertThat(adapter.linksOf(child)).singleElement().extracting(link -> link.excluded()).isEqualTo(true);
		} finally {
			jdbc.update("delete from back_users where id = ?", user);
		}
	}
}
