package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un élément d'une liste déjà dans le bon ordre (SCRUM-176, BACK-46).
 *
 * <p><b>Business :</b> le professeur saisit le texte à la place qu'il veut.
 * Si la frise est active, {@code axisLabel} est le jalon (ex. « 20 minutes »).
 * <p><b>Technique :</b> table {@code back_subject_question_items}. {@code sortOrder}
 * suit l'ordre saisi, en partant de zéro.
 * <p><b>Métier / flow :</b> plusieurs lignes pour une {@link BackSubjectQuestionEntity}
 * dont la forme est {@code order}.
 */
@Entity
@Table(name = "back_subject_question_items")
public class BackSubjectQuestionItemEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Column(length = 200)
	private String label;

	@Column(name = "image_webp")
	private byte[] imageWebp;

	@Column(name = "axis_label", length = 80)
	private String axisLabel;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectQuestionItemEntity() {
	}

	public BackSubjectQuestionItemEntity(long questionId, String label, String axisLabel, int sortOrder) {
		this(questionId, label, axisLabel, sortOrder, null);
	}

	public BackSubjectQuestionItemEntity(
			long questionId, String label, String axisLabel, int sortOrder, byte[] imageWebp) {
		this.questionId = questionId;
		this.label = label;
		this.axisLabel = axisLabel;
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

	public String getAxisLabel() {
		return axisLabel;
	}
}
