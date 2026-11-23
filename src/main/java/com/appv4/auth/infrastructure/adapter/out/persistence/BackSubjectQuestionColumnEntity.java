package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Colonne d'un classement (cartes → colonnes).
 *
 * <p><b>Business :</b> le professeur nomme chaque bac où l'élève déposera des cartes.
 * <p><b>Technique :</b> table {@code back_subject_question_columns}.
 * <p><b>Métier / flow :</b> écrites avant les cartes d'une question {@code classement}.
 */
@Entity
@Table(name = "back_subject_question_columns")
public class BackSubjectQuestionColumnEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Column(name = "label", nullable = false, length = 200)
	private String label;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectQuestionColumnEntity() {
	}

	public BackSubjectQuestionColumnEntity(long questionId, String label, int sortOrder) {
		this.questionId = questionId;
		this.label = label;
		this.sortOrder = sortOrder;
	}

	public Long getId() {
		return id;
	}

	public long getQuestionId() {
		return questionId;
	}

	public String getLabel() {
		return label;
	}

	public int getSortOrder() {
		return sortOrder;
	}
}
