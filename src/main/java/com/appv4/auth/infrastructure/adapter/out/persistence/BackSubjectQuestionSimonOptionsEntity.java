package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Genre des éléments d'une question Simon (BACK-54).
 *
 * <p><b>Business :</b> le professeur choisit couleurs, nombres, lettres ou formes ;
 * l'écran élève affiche les éléments selon ce genre.
 * <p><b>Technique :</b> table 1:1 {@code back_subject_question_simon_options}, clé = id de la question.
 * <p><b>Métier / flow :</b> écrite avant les éléments et la suite par {@link SimonQuestionContentStore}.
 */
@Entity
@Table(name = "back_subject_question_simon_options")
public class BackSubjectQuestionSimonOptionsEntity {

	@Id
	@Column(name = "question_id")
	private long questionId;

	@Column(name = "element_type", nullable = false, length = 20)
	private String elementType;

	protected BackSubjectQuestionSimonOptionsEntity() {
	}

	public BackSubjectQuestionSimonOptionsEntity(long questionId, String elementType) {
		this.questionId = questionId;
		this.elementType = elementType;
	}

	public long getQuestionId() {
		return questionId;
	}

	public String getElementType() {
		return elementType;
	}
}
