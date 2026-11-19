package com.appv4.auth.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.Callable;
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
import org.springframework.transaction.support.TransactionTemplate;

import com.appv4.auth.application.service.UpdateChildProgressAfterAttemptService;

/** PostgreSQL test of concurrent attempts for one child. */
@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("persistence-test")
@Import(ChildProgressPersistenceAdapter.class)
@EnabledIfEnvironmentVariable(named = "APPV4_PERSISTENCE_TEST_URL", matches = ".+")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ChildProgressConcurrencyTest {
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private ChildProgressPersistenceAdapter adapter;
	@Autowired
	private TransactionTemplate transactions;

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> System.getenv("APPV4_PERSISTENCE_TEST_URL"));
		registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_USER", "appv4"));
		registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_PASSWORD", "appv4"));
	}

	@Test
	void concurrentAttemptsAccumulateXpOnFirstAndSubsequentPlays() throws Exception {
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
			jdbc.update("""
					insert into back_subject_question_forms (subject_id, form_code, label, sort_order)
					values (?, 'qcm', 'Choix multiples', 1)
					""", subject);
			long question = jdbc.queryForObject("""
					insert into back_subject_questions (subject_id, form_code, prompt)
					values (?, 'qcm', 'Test') returning id
					""", Long.class, subject);
			var progress = new UpdateChildProgressAfterAttemptService(adapter, Clock.systemUTC());
			for (int round = 1; round <= 2; round++) {
				var inserted = new CountDownLatch(2);
				Callable<Void> attempt = () -> {
					transactions.executeWithoutResult(status -> {
						jdbc.update("""
								insert into back_child_attempts
								(child_id, subject_id, question_id, form_code, score_percent, passed)
								values (?, ?, ?, 'qcm', 100, true)
								""", child, subject, question);
						inserted.countDown();
						awaitBothAttempts(inserted);
						progress.apply(child, 100);
					});
					return null;
				};
				try (var pool = Executors.newFixedThreadPool(2)) {
					var first = pool.submit(attempt);
					var second = pool.submit(attempt);
					first.get(20, TimeUnit.SECONDS);
					second.get(20, TimeUnit.SECONDS);
				}
				assertThat(adapter.findByChildId(child).orElseThrow().xpTotal()).isEqualTo(20 * round);
				assertThat(jdbc.queryForObject("select count(*) from back_child_attempts where child_id = ?",
						Integer.class, child)).isEqualTo(2 * round);
			}
		} finally {
			jdbc.update("delete from back_users where id = ?", user);
		}
	}

	private static void awaitBothAttempts(CountDownLatch inserted) {
		try {
			assertThat(inserted.await(5, TimeUnit.SECONDS)).isTrue();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}
}
