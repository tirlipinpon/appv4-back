package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Une proposition d'un choix multiple (SCRUM-174).
 *
 * <p><b>Business :</b> le texte affiché, et si c'est une bonne réponse.
 * <p><b>Technique :</b> table {@code back_subject_question_choices}. {@code sortOrder}
 * suit l'ordre saisi par le professeur.
 * <p><b>Métier / flow :</b> plusieurs lignes pour une {@link BackSubjectQuestionEntity}.
 */
@Entity
@Table(name = "back_subject_question_choices")
public class BackSubjectQuestionChoiceEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Column(nullable = false, length = 200)
	private String label;

	@Column(nullable = false)
	private boolean correct;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectQuestionChoiceEntity() {
	}

	public BackSubjectQuestionChoiceEntity(long questionId, String label, boolean correct, int sortOrder) {
		this.questionId = questionId;
		this.label = label;
		this.correct = correct;
		this.sortOrder = sortOrder;
	}

	public long getQuestionId() {
		return questionId;
	}

	public Long getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public boolean isCorrect() {
		return correct;
	}
}
