package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.PageClaim;

/**
 * Persiste les tentatives enfant (BACK-16).
 *
 * <p><b>Business :</b> une validation = une ligne attempt + ses réponses.
 * <p><b>Technique :</b> JPA sur {@code back_child_attempts}. Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out du port {@link ChildQuestionAttemptPort}.
 */
@Component
@Profile("!test")
public class ChildQuestionAttemptPersistenceAdapter implements ChildQuestionAttemptPort {

	private final BackChildAttemptJpaRepository attempts;
	private final BackChildAttemptAnswerJpaRepository answers;
	private final JdbcClient jdbc;

	public ChildQuestionAttemptPersistenceAdapter(
			BackChildAttemptJpaRepository attempts, BackChildAttemptAnswerJpaRepository answers, JdbcClient jdbc) {
		this.attempts = attempts;
		this.answers = answers;
		this.jdbc = jdbc;
	}

	@Override
	@Transactional
	public Optional<StoredAttempt> lockAndFind(long childId, UUID submissionId) {
		jdbc.sql("select id from back_children where id = :child for no key update")
				.param("child", childId).query(Long.class).optional();
		return attempts.findByChildIdAndSubmissionId(childId, submissionId).map(ChildQuestionAttemptPersistenceAdapter::stored);
	}

	@Override
	@Transactional
	public Optional<PageClaim> claim(UUID playId, long childId, long subjectId, long questionId, int maxAttempts) {
		jdbc.sql("""
				insert into back_child_play_pages (play_id, child_id, subject_id, question_id, attempts_used)
				values (:playId, :childId, :subjectId, :questionId, 0)
				on conflict (play_id) do nothing
				""")
				.param("playId", playId)
				.param("childId", childId)
				.param("subjectId", subjectId)
				.param("questionId", questionId)
				.update();
		return jdbc.sql("""
				select attempts_used from back_child_play_pages
				where play_id = :playId and child_id = :childId
				  and subject_id = :subjectId and question_id = :questionId
				for update
				""")
				.param("playId", playId)
				.param("childId", childId)
				.param("subjectId", subjectId)
				.param("questionId", questionId)
				.query(Integer.class)
				.optional()
				.filter(used -> used < maxAttempts)
				.map(used -> {
					int rank = used + 1;
					jdbc.sql("update back_child_play_pages set attempts_used = :used where play_id = :playId")
							.param("used", rank)
							.param("playId", playId)
							.update();
					return new PageClaim(rank, maxAttempts - rank);
				});
	}

	@Override
	public int attemptsUsed(UUID playId, long childId, long subjectId, long questionId) {
		return jdbc.sql("""
				select attempts_used from back_child_play_pages
				where play_id = :playId and child_id = :childId
				  and subject_id = :subjectId and question_id = :questionId
				""")
				.param("playId", playId)
				.param("childId", childId)
				.param("subjectId", subjectId)
				.param("questionId", questionId)
				.query(Integer.class)
				.optional()
				.orElse(0);
	}

	@Override
	@Transactional
	public StoredAttempt insert(NewAttempt attempt) {
		BackChildAttemptEntity saved = attempts.save(new BackChildAttemptEntity(
				attempt.childId(),
				attempt.subjectId(),
				attempt.questionId(),
				attempt.formCode(),
				attempt.scorePercent(),
				attempt.passed(),
				attempt.submissionId(),
				attempt.attemptRank(),
				attempt.attemptsRemaining(),
				attempt.pointsAwarded()));
		long attemptId = saved.getId();
		for (AnswerLine line : attempt.answers()) {
			answers.save(new BackChildAttemptAnswerEntity(
					attemptId,
					line.targetKind(),
					line.targetId(),
					line.answerBool(),
					line.answerChoiceId(),
					line.correct()));
		}
		return stored(saved);
	}

	private static StoredAttempt stored(BackChildAttemptEntity saved) {
		return new StoredAttempt(
				saved.getId(),
				saved.getChildId(),
				saved.getSubjectId(),
				saved.getQuestionId(),
				saved.getFormCode(),
				saved.getScorePercent(),
				saved.isPassed(),
				saved.getAttemptRank(),
				saved.getAttemptsRemaining(),
				saved.getPointsAwarded());
	}
}
