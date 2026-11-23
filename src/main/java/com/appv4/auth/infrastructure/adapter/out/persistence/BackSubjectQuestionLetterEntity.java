package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Réponse attendue d'une saisie lettre par lettre (SCRUM-182).
 *
 * <p><b>Business :</b> le professeur note le mot juste, par exemple « Paris ».
 * L'élève le compose dans une case par lettre.
 * <p><b>Technique :</b> table {@code back_subject_question_letters}. Une ligne par question.
 * <p><b>Métier / flow :</b> écrite avec la question dont la forme est {@code reponse_libre}.
 */
@Entity
@Table(name = "back_subject_question_letters")
public class BackSubjectQuestionLetterEntity {

	@Id
	@Column(name = "question_id", nullable = false)
	private Long questionId;

	@Column(name = "expected_answer", nullable = false, length = 500)
	private String expectedAnswer;

	protected BackSubjectQuestionLetterEntity() {
	}

	public BackSubjectQuestionLetterEntity(long questionId, String expectedAnswer) {
		this.questionId = questionId;
		this.expectedAnswer = expectedAnswer;
	}

	public Long getQuestionId() {
		return questionId;
	}

	public String getExpectedAnswer() {
		return expectedAnswer;
	}
}
