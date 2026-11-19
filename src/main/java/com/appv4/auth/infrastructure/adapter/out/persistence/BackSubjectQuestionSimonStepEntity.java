package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Pas de la suite d'une question Simon (BACK-54).
 *
 * <p><b>Business :</b> l'ordre dans lequel les éléments s'allument, celui que l'élève doit refaire.
 * <p><b>Technique :</b> table {@code back_subject_question_simon_steps}. {@code elementIndex} est le
 * {@code sort_order} d'un élément de la même question (clé étrangère composée en base).
 * <p><b>Métier / flow :</b> écrits après les éléments par {@link SimonQuestionContentStore}.
 */
@Entity
@Table(name = "back_subject_question_simon_steps")
public class BackSubjectQuestionSimonStepEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Column(name = "element_index", nullable = false)
	private int elementIndex;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectQuestionSimonStepEntity() {
	}

	public BackSubjectQuestionSimonStepEntity(long questionId, int elementIndex, int sortOrder) {
		this.questionId = questionId;
		this.elementIndex = elementIndex;
		this.sortOrder = sortOrder;
	}

	public Long getId() {
		return id;
	}

	public long getQuestionId() {
		return questionId;
	}

	public int getElementIndex() {
		return elementIndex;
	}

	public int getSortOrder() {
		return sortOrder;
	}
}
