package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Une phrase vraie ou fausse préparée par le professeur (SCRUM-177).
 *
 * <p><b>Business :</b> le texte de la phrase, et si elle est vraie.
 * L'élève ne choisit pas encore un côté.
 * <p><b>Technique :</b> table {@code back_subject_question_statements}. {@code sortOrder}
 * suit l'ordre saisi, en partant de zéro.
 * <p><b>Métier / flow :</b> plusieurs lignes pour une {@link BackSubjectQuestionEntity}
 * dont la forme est {@code vrai_faux}.
 */
@Entity
@Table(name = "back_subject_question_statements")
public class BackSubjectQuestionStatementEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Column(nullable = false, length = 200)
	private String label;

	@Column(nullable = false)
	private boolean truthful;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectQuestionStatementEntity() {
	}

	public BackSubjectQuestionStatementEntity(long questionId, String label, boolean truthful, int sortOrder) {
		this.questionId = questionId;
		this.label = label;
		this.truthful = truthful;
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

	public boolean isTruthful() {
		return truthful;
	}
}
