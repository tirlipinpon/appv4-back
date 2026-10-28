package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un mot déjà relié à sa réponse (SCRUM-178).
 *
 * <p><b>Business :</b> le professeur saisit le mot et la réponse ensemble.
 * L'élève ne trace pas encore le trait.
 * <p><b>Technique :</b> table {@code back_subject_question_links}. {@code sortOrder}
 * suit l'ordre saisi, en partant de zéro.
 * <p><b>Métier / flow :</b> plusieurs lignes pour une {@link BackSubjectQuestionEntity}
 * dont la forme est {@code liens}.
 */
@Entity
@Table(name = "back_subject_question_links")
public class BackSubjectQuestionLinkEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Column(name = "left_label", nullable = false, length = 200)
	private String leftLabel;

	@Column(name = "right_label", nullable = false, length = 200)
	private String rightLabel;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectQuestionLinkEntity() {
	}

	public BackSubjectQuestionLinkEntity(long questionId, String leftLabel, String rightLabel, int sortOrder) {
		this.questionId = questionId;
		this.leftLabel = leftLabel;
		this.rightLabel = rightLabel;
		this.sortOrder = sortOrder;
	}

	public long getQuestionId() {
		return questionId;
	}

	public Long getId() {
		return id;
	}

	public String getLeftLabel() {
		return leftLabel;
	}

	public String getRightLabel() {
		return rightLabel;
	}
}
