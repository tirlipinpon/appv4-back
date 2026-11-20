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

	@Column(nullable = true, length = 200)
	private String label;

	@Column(name = "image_webp")
	private byte[] imageWebp;

	@Column(nullable = false)
	private boolean correct;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectQuestionChoiceEntity() {
	}

	public BackSubjectQuestionChoiceEntity(long questionId, String label, boolean correct, int sortOrder) {
		this(questionId, label, correct, sortOrder, null);
	}

	public BackSubjectQuestionChoiceEntity(
			long questionId, String label, boolean correct, int sortOrder, byte[] imageWebp) {
		this.questionId = questionId;
		this.label = label;
		this.correct = correct;
		this.sortOrder = sortOrder;
		this.imageWebp = imageWebp;
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

	public byte[] getImageWebp() {
		return imageWebp;
	}

	public boolean isCorrect() {
		return correct;
	}
}
