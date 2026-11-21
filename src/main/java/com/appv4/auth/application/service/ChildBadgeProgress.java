package com.appv4.auth.application.service;

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

	/** Relit les étoiles après l'enregistrement de l'essai. */
	void afterMarksChanged(long childId);
}
