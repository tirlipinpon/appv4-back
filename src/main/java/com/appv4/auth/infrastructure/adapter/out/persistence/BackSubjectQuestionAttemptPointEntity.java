package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Points d'un essai quand le professeur les personnalise (BACK-45).
 *
 * <p><b>Business :</b> un nombre par essai, dans l'ordre. Le premier essai est 0.
 * <p><b>Technique :</b> table {@code back_subject_question_attempt_points}.
 * <p><b>Métier / flow :</b> écrite seulement si {@code customize_points} est vrai.
 */
@Entity
@Table(name = "back_subject_question_attempt_points")
@IdClass(BackSubjectQuestionAttemptPointKey.class)
public class BackSubjectQuestionAttemptPointEntity {

	@Id
	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Id
	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private int points;

	protected BackSubjectQuestionAttemptPointEntity() {
	}

	public BackSubjectQuestionAttemptPointEntity(long questionId, int sortOrder, int points) {
		this.questionId = questionId;
		this.sortOrder = sortOrder;
		this.points = points;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public int getPoints() {
		return points;
	}
}
