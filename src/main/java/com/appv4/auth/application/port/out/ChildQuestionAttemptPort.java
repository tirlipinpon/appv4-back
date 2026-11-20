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

	/**
	 * Réserve le prochain essai de la page ouverte.
	 *
	 * <p><b>Business :</b> deux validations simultanées de la même page ne doivent pas dépasser le plafond.
	 * <p><b>Technique :</b> l'adaptateur verrouille la ligne de la page.
	 * <p><b>Métier / flow :</b> submit, après la correction, avant l'insert.
	 *
	 * @return vide si la page a déjà épuisé ses essais
	 */
	Optional<PageClaim> claim(UUID playId, long childId, long subjectId, long questionId, int maxAttempts);

	/**
	 * Essais déjà pris sur cette page. Zéro si la page n'a pas encore validé.
	 *
	 * <p><b>Business :</b> rouvrir la même page doit montrer ce qu'il reste vraiment.
	 * <p><b>Technique :</b> lecture de {@code back_child_play_pages}, sans verrou.
	 * <p><b>Métier / flow :</b> allowance, avant d'afficher la question.
	 */
	int attemptsUsed(UUID playId, long childId, long subjectId, long questionId);

	/** Rang 1 = premier essai de la page. {@code attemptsRemaining} est ce qu'il reste après celui-ci. */
	record PageClaim(int rank, int attemptsRemaining) {
	}

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
			UUID submissionId,
			Integer attemptRank,
			Integer attemptsRemaining,
			Integer pointsAwarded) {
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
			boolean passed,
			Integer attemptRank,
			Integer attemptsRemaining,
			Integer pointsAwarded) {
		public StoredAttempt(
				long id, long childId, long subjectId, long questionId, String formCode, int scorePercent, boolean passed) {
			this(id, childId, subjectId, questionId, formCode, scorePercent, passed, null, null, null);
		}
	}
}
