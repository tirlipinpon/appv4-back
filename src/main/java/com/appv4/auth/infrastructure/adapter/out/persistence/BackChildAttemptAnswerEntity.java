package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Détail d'une réponse dans une tentative enfant (BACK-16).
 *
 * <p><b>Business :</b> ce que l'enfant a choisi, et si c'était juste.
 * <p><b>Technique :</b> table {@code back_child_attempt_answers}.
 * <p><b>Métier / flow :</b> insert avec la tentative parente.
 */
@Entity
@Table(name = "back_child_attempt_answers")
public class BackChildAttemptAnswerEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "attempt_id", nullable = false)
	private long attemptId;

	@Column(name = "target_kind", nullable = false, length = 32)
	private String targetKind;

	@Column(name = "target_id", nullable = false)
	private long targetId;

	@Column(name = "answer_bool")
	private Boolean answerBool;

	@Column(name = "answer_choice_id")
	private Long answerChoiceId;

	@Column(nullable = false)
	private boolean correct;

	protected BackChildAttemptAnswerEntity() {
	}

	public BackChildAttemptAnswerEntity(
			long attemptId,
			String targetKind,
			long targetId,
			Boolean answerBool,
			Long answerChoiceId,
			boolean correct) {
		this.attemptId = attemptId;
		this.targetKind = targetKind;
		this.targetId = targetId;
		this.answerBool = answerBool;
		this.answerChoiceId = answerChoiceId;
		this.correct = correct;
	}
}
