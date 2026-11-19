package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Élément (bouton) d'une question Simon (BACK-54).
 *
 * <p><b>Business :</b> ce que l'élève voit s'allumer puis touche : rouge, 7, B, cercle.
 * <p><b>Technique :</b> table {@code back_subject_question_simon_elements}. {@code sortOrder} est
 * la position que les pas de la suite désignent ; {@code color} n'existe que pour les couleurs.
 * <p><b>Métier / flow :</b> écrits avant la suite d'une question {@code simon}.
 */
@Entity
@Table(name = "back_subject_question_simon_elements")
public class BackSubjectQuestionSimonElementEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Column(name = "label", nullable = false, length = 200)
	private String label;

	@Column(name = "color", length = 7)
	private String color;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectQuestionSimonElementEntity() {
	}

	public BackSubjectQuestionSimonElementEntity(long questionId, String label, String color, int sortOrder) {
		this.questionId = questionId;
		this.label = label;
		this.color = color;
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

	public String getColor() {
		return color;
	}

	public int getSortOrder() {
		return sortOrder;
	}
}
