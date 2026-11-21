package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AttemptTrace;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.PageClaim;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredMark;
import com.appv4.auth.application.port.out.QuestionMark;

/**
 * Persiste les tentatives enfant (BACK-16).
 *
 * <p><b>Business :</b> une validation = une ligne attempt + ses réponses, et l'étoile
 * courante de la question pour cet enfant.
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
	public Optional<StoredMark> findMark(long childId, long questionId) {
		return readMark(childId, questionId, false);
	}

	@Override
	public Optional<StoredMark> lockMark(long childId, long questionId) {
		return readMark(childId, questionId, true);
	}

	private Optional<StoredMark> readMark(long childId, long questionId, boolean lock) {
		String sql = """
				select mark_code, yellow_count from back_child_question_marks
				where child_id = :childId and question_id = :questionId
				""";
		if (lock) {
			sql = sql + " for update";
		}
		return jdbc.sql(sql)
				.param("childId", childId)
				.param("questionId", questionId)
				.query((rs, row) -> readStored(rs))
				.optional();
	}

	@Override
	@Transactional
	public void saveMark(long childId, long questionId, QuestionMark code, int yellowCount) {
		jdbc.sql("""
				insert into back_child_question_marks (child_id, question_id, mark_code, yellow_count)
				values (:childId, :questionId, :code, :yellowCount)
				on conflict (child_id, question_id) do update
				set mark_code = excluded.mark_code, yellow_count = excluded.yellow_count
				""")
				.param("childId", childId)
				.param("questionId", questionId)
				.param("code", code.wire())
				.param("yellowCount", yellowCount)
				.update();
	}

	@Override
	public Map<Long, StoredMark> marksFor(long childId, Collection<Long> questionIds) {
		if (questionIds == null || questionIds.isEmpty()) {
			return Map.of();
		}
		Map<Long, StoredMark> found = new HashMap<>();
		jdbc.sql("""
				select question_id, mark_code, yellow_count from back_child_question_marks
				where child_id = :childId and question_id in (:questionIds)
				""")
				.param("childId", childId)
				.param("questionIds", questionIds)
				.query((rs, row) -> {
					found.put(rs.getLong("question_id"), readStored(rs));
					return null;
				})
				.list();
		return found;
	}

	@Override
	public Map<Long, StoredMark> allMarks(long childId) {
		Map<Long, StoredMark> found = new HashMap<>();
		jdbc.sql("""
				select question_id, mark_code, yellow_count from back_child_question_marks
				where child_id = :childId
				""")
				.param("childId", childId)
				.query((rs, row) -> {
					found.put(rs.getLong("question_id"), readStored(rs));
					return null;
				})
				.list();
		return found;
	}

	@Override
	public List<AttemptTrace> traces(long childId) {
		List<AttemptTrace> found = new java.util.ArrayList<>();
		jdbc.sql("""
				select question_id, created_at, mark_code, passed, pay
				from back_child_attempts
				where child_id = :childId
				order by created_at, id
				""")
				.param("childId", childId)
				.query((rs, row) -> {
					String mark = rs.getString("mark_code");
					found.add(new AttemptTrace(
							rs.getLong("question_id"),
							rs.getTimestamp("created_at").toInstant(),
							mark == null ? null : QuestionMark.fromWire(mark),
							rs.getBoolean("passed"),
							(Boolean) rs.getObject("pay")));
					return null;
				})
				.list();
		return found;
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
				attempt.pointsAwarded(),
				attempt.markCode() == null ? null : attempt.markCode().wire(),
				attempt.yellowCount(),
				attempt.pay()));
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

	private static StoredMark readStored(java.sql.ResultSet rs) throws java.sql.SQLException {
		return new StoredMark(QuestionMark.fromWire(rs.getString("mark_code")), rs.getInt("yellow_count"));
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
				saved.getPointsAwarded(),
				saved.getMarkCode() == null ? null : QuestionMark.fromWire(saved.getMarkCode()),
				saved.getYellowCount(),
				saved.getPay());
	}
}
