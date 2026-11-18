package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;

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
 * <p><b>Technique :</b> table {@code back_child_attempts}.
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
	private int scorePercent;

	@Column(nullable = false)
	private boolean passed;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected BackChildAttemptEntity() {
	}

	public BackChildAttemptEntity(
			long childId, long subjectId, long questionId, String formCode, int scorePercent, boolean passed) {
		this.childId = childId;
		this.subjectId = subjectId;
		this.questionId = questionId;
		this.formCode = formCode;
		this.scorePercent = scorePercent;
		this.passed = passed;
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
}
