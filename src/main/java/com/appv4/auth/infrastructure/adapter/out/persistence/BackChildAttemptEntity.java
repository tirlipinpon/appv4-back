package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tentative enfant sur une question (BACK-16).
 *
 * <p><b>Business :</b> trace d'une partie pour score et lectures futures.
 * L'étoile et son compte sont ceux de cet essai, pour qu'une relance ne les recalcule pas.
 * <p><b>Technique :</b> table {@code back_child_attempts}. Le score est un {@code SMALLINT}
 * (0–100), comme l'âge sur {@link BackChildEntity} : un {@code int} Java ferait échouer
 * {@code ddl-auto: validate} (Hibernate attendrait un {@code INTEGER}).
 * <p><b>Métier / flow :</b> adapter out du port attempts.
 */
@Entity
@Table(name = "back_child_attempts")
public class BackChildAttemptEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "child_id", nullable = false)
	private long childId;

	@Column(name = "subject_id", nullable = false)
	private long subjectId;

	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Column(name = "form_code", nullable = false, length = 40)
	private String formCode;

	@Column(name = "score_percent", nullable = false)
	private short scorePercent;

	@Column(nullable = false)
	private boolean passed;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "submission_id")
	private UUID submissionId;

	@Column(name = "attempt_rank", nullable = true)
	private Integer attemptRank;

	@Column(name = "attempts_remaining", nullable = true)
	private Integer attemptsRemaining;

	@Column(name = "points_awarded", nullable = true)
	private Integer pointsAwarded;

	@Column(name = "mark_code", length = 16)
	private String markCode;

	@Column(name = "yellow_count")
	private Integer yellowCount;

	/** Décision pay de l'XP. Null : essai ancien, les badges qui suivent pay l'ignorent. */
	@Column(name = "pay")
	private Boolean pay;

	protected BackChildAttemptEntity() {
	}

	public BackChildAttemptEntity(
			long childId, long subjectId, long questionId, String formCode, int scorePercent, boolean passed,
			UUID submissionId) {
		this(childId, subjectId, questionId, formCode, scorePercent, passed, submissionId, null, null, null);
	}

	public BackChildAttemptEntity(
			long childId, long subjectId, long questionId, String formCode, int scorePercent, boolean passed,
			UUID submissionId, Integer attemptRank, Integer attemptsRemaining, Integer pointsAwarded) {
		this(childId, subjectId, questionId, formCode, scorePercent, passed, submissionId,
				attemptRank, attemptsRemaining, pointsAwarded, null, null, null);
	}

	public BackChildAttemptEntity(
			long childId, long subjectId, long questionId, String formCode, int scorePercent, boolean passed,
			UUID submissionId, Integer attemptRank, Integer attemptsRemaining, Integer pointsAwarded,
			String markCode, Integer yellowCount, Boolean pay) {
		this.childId = childId;
		this.subjectId = subjectId;
		this.questionId = questionId;
		this.formCode = formCode;
		this.scorePercent = (short) scorePercent;
		this.passed = passed;
		this.submissionId = submissionId;
		this.attemptRank = attemptRank;
		this.attemptsRemaining = attemptsRemaining;
		this.pointsAwarded = pointsAwarded;
		this.markCode = markCode;
		this.yellowCount = yellowCount;
		this.pay = pay;
	}

	public Boolean getPay() {
		return pay;
	}

	public Long getId() {
		return id;
	}

	public long getChildId() {
		return childId;
	}

	public long getSubjectId() {
		return subjectId;
	}

	public long getQuestionId() {
		return questionId;
	}

	public String getFormCode() {
		return formCode;
	}

	public int getScorePercent() {
		return scorePercent;
	}

	public boolean isPassed() {
		return passed;
	}

	public Integer getAttemptRank() {
		return attemptRank;
	}

	public Integer getAttemptsRemaining() {
		return attemptsRemaining;
	}

	public Integer getPointsAwarded() {
		return pointsAwarded;
	}

	public String getMarkCode() {
		return markCode;
	}

	public Integer getYellowCount() {
		return yellowCount;
	}
}
