package com.appv4.auth.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.appv4.auth.application.service.UpdateChildProgressAfterAttemptService;
import com.appv4.auth.application.service.ChildDailyMissionService;
import com.appv4.auth.application.service.ChildQuestionPlayService;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.Simon;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.Submission;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort.ChildSession;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredSimon;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredSimonElement;
import com.appv4.auth.application.result.ChildAttemptResultView;

/** PostgreSQL test of concurrent attempts for one child. */
@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("persistence-test")
@Import(ChildProgressPersistenceAdapter.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ChildProgressConcurrencyIT {
	@Autowired
	private JdbcTemplate jdbc;
	@Autowired
	private ChildProgressPersistenceAdapter adapter;
	@Autowired
	private TransactionTemplate transactions;
	@Autowired
	private BackChildAttemptJpaRepository attempts;
	@Autowired
	private BackChildAttemptAnswerJpaRepository answers;

	@DynamicPropertySource
	static void database(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> System.getenv("APPV4_PERSISTENCE_TEST_URL"));
		registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_USER", "appv4"));
		registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("APPV4_PERSISTENCE_TEST_PASSWORD", "appv4"));
	}

	@Test
	void apply_accumulatesXp_whenAttemptsCommitConcurrently() throws Exception {
		long user = user();
		try {
			Fixture fixture = seed(user, "qcm");
			long child = fixture.child();
			long subject = fixture.subject();
			long question = fixture.question();
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

	@Test
	void simonCorrelatesOpeningsAndCreditsOneSubmissionOnce() throws Exception {
		long user = user();
		try {
			Fixture fixture = seed(user, "simon");
			JdbcClient client = JdbcClient.create(jdbc);
			var challenges = new ChildSimonChallengeAdapter(client);
			var attemptPort = new ChildQuestionAttemptPersistenceAdapter(attempts, answers, client);
			var game = game(fixture, challenges, attemptPort);
			List<Integer> firstSequence = List.of(0, 1, 2);
			var firstId = challenges.issue(fixture.child(), fixture.question(), firstSequence, false);
			var secondId = challenges.issue(fixture.child(), fixture.question(), List.of(2, 1, 0), false);
			Submission first = new Submission("simon", new Simon(firstSequence), UUID.randomUUID(), firstId);
			var start = new CountDownLatch(2);
			Callable<ChildAttemptResultView> send = () -> {
				start.countDown();
				awaitBothAttempts(start);
				return send(game, fixture, first);
			};
			try (var pool = Executors.newFixedThreadPool(2)) {
				var left = pool.submit(send);
				var right = pool.submit(send);
				ChildAttemptResultView verdict = left.get(20, TimeUnit.SECONDS);
				assertThat(verdict.passed()).isTrue();
				assertThat(right.get(20, TimeUnit.SECONDS)).isEqualTo(verdict);
			}
			assertThat(adapter.findByChildId(fixture.child()).orElseThrow().xpTotal()).isEqualTo(10);
			assertThat(send(game, fixture, new Submission(
					"simon", new Simon(List.of(2, 1, 0)), UUID.randomUUID(), secondId)).passed()).isTrue();
			assertThatThrownBy(() -> send(game, fixture, new Submission(
					"simon", new Simon(firstSequence), UUID.randomUUID(), firstId)))
					.isInstanceOf(AuthDomainException.class);
			assertThat(jdbc.queryForObject("select count(*) from back_child_attempts where child_id = ?",
					Integer.class, fixture.child())).isEqualTo(2);
		} finally {
			jdbc.update("delete from back_users where id = ?", user);
		}
	}

	private ChildQuestionPlayService game(Fixture fixture, ChildSimonChallengeAdapter challenges,
			ChildQuestionAttemptPersistenceAdapter attemptPort) {
		var child = mock(ChildLoginPort.class);
		when(child.findActiveById(fixture.child()))
				.thenReturn(Optional.of(new ChildLoginCandidate(fixture.child(), "Test", "hash")));
		var session = mock(ChildSessionTokenPort.class);
		when(session.read("fixture")).thenReturn(Optional.of(new ChildSession(fixture.child())));
		var subjects = mock(ChildSubjectPort.class);
		when(subjects.linksOf(fixture.child())).thenReturn(List.of(new StoredLink(fixture.subject(), false)));
		when(subjects.visibleByIds(List.of(fixture.subject())))
				.thenReturn(List.of(new CatalogSubject(fixture.subject(), "Test", "Test", 1L, "CE1", "math")));
		var questions = mock(TeacherQuestionPort.class);
		when(questions.findInSubject(fixture.subject(), fixture.question()))
				.thenReturn(Optional.of(new StoredQuestion(fixture.question(), fixture.subject(), "simon", "",
						new StoredSimon("nombres", List.of(
								new StoredSimonElement(1L, "1", null),
								new StoredSimonElement(2L, "2", null),
								new StoredSimonElement(3L, "3", null))))));
		var progress = new UpdateChildProgressAfterAttemptService(adapter, Clock.systemUTC());
		return new ChildQuestionPlayService(child, session, subjects, questions, attemptPort, challenges,
				progress, mock(ChildDailyMissionService.class));
	}

	private ChildAttemptResultView send(ChildQuestionPlayService game, Fixture fixture, Submission submission) {
		return transactions.execute(status -> game.submit("fixture", fixture.subject(), fixture.question(), submission));
	}

	private long user() {
		return jdbc.queryForObject("insert into back_users (email, display_name) values (?, 'Test') returning id",
				Long.class, UUID.randomUUID() + "@example.test");
	}

	private Fixture seed(long user, String form) {
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
				values (?, ?, 'Test', 1)
				""", subject, form);
		long question = jdbc.queryForObject("""
				insert into back_subject_questions (subject_id, form_code, prompt)
				values (?, ?, 'Test') returning id
				""", Long.class, subject, form);
		return new Fixture(child, subject, question);
	}

	private record Fixture(long child, long subject, long question) {
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
