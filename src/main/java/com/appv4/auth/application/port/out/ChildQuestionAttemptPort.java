package com.appv4.auth.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;
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

	/**
	 * Étoile actuelle de cette question pour cet enfant, sans verrou.
	 *
	 * <p><b>Business :</b> pas de ligne veut dire pas d'étoile. L'enfant revoit la même marque en rouvrant.
	 * <p><b>Technique :</b> lecture de {@code back_child_question_marks}, sans {@code FOR UPDATE}.
	 * Un compte écrit dans la transaction en cours peut manquer.
	 * <p><b>Métier / flow :</b> lecture hors du calcul qui ajoute une étoile jaune.
	 */
	Optional<StoredMark> findMark(long childId, long questionId);

	/**
	 * Relit l'étoile en verrouillant la ligne, puis rend le compte déjà écrit.
	 *
	 * <p><b>Business :</b> deux réussites en même temps ne doivent pas rejouer le même compte
	 * et en perdre une.
	 * <p><b>Technique :</b> {@code SELECT … FOR UPDATE} sur {@code back_child_question_marks}.
	 * Pas de ligne : rien à verrouiller, l'appelant part de zéro.
	 * <p><b>Métier / flow :</b> submit, dans la transaction, avant de calculer le nouveau compte.
	 */
	Optional<StoredMark> lockMark(long childId, long questionId);

	/**
	 * Enregistre l'étoile noire (compte zéro) ou jaune (compte un ou plus).
	 *
	 * <p><b>Business :</b> une seule marque par enfant et par question. La jaune remplace la noire.
	 * <p><b>Technique :</b> upsert sur {@code back_child_question_marks}.
	 * <p><b>Métier / flow :</b> après la décision du jeu, avant la réponse au front.
	 */
	void saveMark(long childId, long questionId, QuestionMark code, int yellowCount);

	/**
	 * Marques des questions demandées. Une question absente n'a pas d'étoile.
	 *
	 * <p><b>Business :</b> la liste des questions montre l'étoile avant d'ouvrir la page.
	 * <p><b>Technique :</b> lecture groupée, sans créer de ligne.
	 * <p><b>Métier / flow :</b> use case liste → cet appel → carte de question.
	 */
	Map<Long, StoredMark> marksFor(long childId, Collection<Long> questionIds);

	/**
	 * Toutes les étoiles de cet enfant.
	 *
	 * <p><b>Business :</b> la somme des étoiles jaunes nourrit les badges, pas une liste de questions.
	 * <p><b>Technique :</b> lecture de {@code back_child_question_marks} pour un enfant.
	 * <p><b>Métier / flow :</b> lecture des badges, avant de décider un niveau.
	 */
	Map<Long, StoredMark> allMarks(long childId);

	/** Rang 1 = premier essai de la page. {@code attemptsRemaining} est ce qu'il reste après celui-ci. */
	record PageClaim(int rank, int attemptsRemaining) {
	}

	/**
	 * Étoile stockée. {@code code} vaut {@code black} ou {@code yellow}.
	 *
	 * <p><b>Business :</b> le noir ne se cumule pas. Le jaune compte les maîtrises au premier essai.
	 * <p><b>Technique :</b> pas de code {@code none} en base : l'absence de ligne suffit.
	 * <p><b>Métier / flow :</b> écrite par le jeu, relue par le jeu et par la liste.
	 */
	record StoredMark(QuestionMark code, int yellowCount) {
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
			Integer pointsAwarded,
			QuestionMark markCode,
			Integer yellowCount,
			Boolean pay) {
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
			Integer pointsAwarded,
			QuestionMark markCode,
			Integer yellowCount,
			Boolean pay) {
		public StoredAttempt(
				long id, long childId, long subjectId, long questionId, String formCode, int scorePercent, boolean passed) {
			this(id, childId, subjectId, questionId, formCode, scorePercent, passed, null, null, null, null, null, null);
		}
	}
}
