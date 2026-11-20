package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistance des tentatives enfant (BACK-16).
 *
 * <p><b>Business :</b> chaque validation laisse une trace pour la progression.
 * <p><b>Technique :</b> tables {@code back_child_attempts} / {@code back_child_attempt_answers}.
 * <p><b>Métier / flow :</b> use case play → insert → score renvoyé au front.
 */
public interface ChildQuestionAttemptPort {

	StoredAttempt insert(NewAttempt attempt);

	/** Sérialise les soumissions rejouables de cet enfant jusqu'à la fin de la transaction. */
	Optional<StoredAttempt> lockAndFind(long childId, UUID submissionId);

	record AnswerLine(
			String targetKind,
			long targetId,
			Boolean answerBool,
			Long answerChoiceId,
			boolean correct) {
	}

	record NewAttempt(
			long childId,
			long subjectId,
			long questionId,
			String formCode,
			int scorePercent,
			boolean passed,
			List<AnswerLine> answers,
			UUID submissionId) {
		public NewAttempt {
			answers = answers == null ? List.of() : List.copyOf(answers);
		}
	}

	record StoredAttempt(
			long id,
			long childId,
			long subjectId,
			long questionId,
			String formCode,
			int scorePercent,
			boolean passed) {
	}
}
