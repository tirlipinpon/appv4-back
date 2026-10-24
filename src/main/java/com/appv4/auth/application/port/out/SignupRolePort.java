package com.appv4.auth.application.port.out;

/**
 * Port sortant — rôles posés à l'inscription email (SCRUM-121).
 *
 * <p><b>Business :</b> parent est un vrai rôle tout de suite. Professeur n'est
 * qu'une demande en attente : personne n'entre en classe sans un admin.
 * <p><b>Technique :</b> {@code back_user_roles} et {@code back_teacher_role_requests}.
 * <p><b>Métier / flow :</b> appelé par le signup, après la création du compte,
 * avant le JWT. N'écrit jamais le rôle {@code teacher}.
 */
public interface SignupRolePort {

	/**
	 * Donne le rôle parent. Une deuxième fois ne crée pas de doublon.
	 */
	void grantParent(long userId);

	/**
	 * Ouvre une demande professeur {@code pending} s'il n'y en a pas déjà une.
	 * Ne crée pas de ligne {@code teacher} dans {@code back_user_roles}.
	 */
	void requestTeacher(long userId);

	/**
	 * Vrai si le premier choix parent/professeur a déjà été enregistré (issue #35).
	 * Indépendant des rôles encore actifs (ex. après révocation teacher-only).
	 */
	boolean hasCompletedInitialChoice(long userId);

	/**
	 * Marque le choix initial comme terminé. Idempotent.
	 */
	void markInitialChoiceCompleted(long userId);
}
