package com.appv4.auth.application.service;

import com.appv4.auth.application.port.out.QuestionMark;

/**
 * Mise à jour des badges autour d'un essai enregistré (BACK-19).
 *
 * <p><b>Business :</b> un essai refusé n'existe pas, donc il ne change aucun badge.
 * <p><b>Technique :</b> appelée par le jeu juste avant puis juste après l'enregistrement.
 * <p><b>Métier / flow :</b> submit → avant l'essai (reprise historique) → étoile → après l'essai.
 */
public interface ChildBadgeProgress {

	/** Reprend les badges déjà prouvés avant d'appliquer l'essai en cours. */
	void beforeAttempt(long childId);

	/**
	 * Relit les étoiles après l'essai.
	 *
	 * <p><b>Business :</b> Matière au jaune ne monte que si la marque passe de aucune
	 * ou noire à jaune, puis que la matière est toute jaune.
	 */
	void afterAttempt(long childId, long subjectId, long questionId, QuestionMark before, QuestionMark after);

	/** Relit la mission du jour après qu'une tentative a pu la terminer. */
	void noteMission(long childId);
}
