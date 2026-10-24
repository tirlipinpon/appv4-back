package com.appv4.auth.application.exception;

/**
 * Codes d'erreur auth stables (issue #30) — le mapping HTTP est dans l'adaptateur web.
 *
 * <p>Aligné en esprit avec {@link com.appv4.shared.domain.ErrorCode}.
 */
public enum AuthErrorCode {
	/** Identifiants / jeton / state OAuth invalides. */
	UNAUTHORIZED,
	/** Droit manquant (rôle, espace, admin). */
	FORBIDDEN,
	/** Entrée métier invalide. */
	BAD_REQUEST,
	/** Ressource inconnue. */
	NOT_FOUND,
	/** Conflit d'unicité / état concurrent. */
	CONFLICT,
	/** Fournisseur OAuth ou dépendance externe non configurée. */
	SERVICE_UNAVAILABLE,
	/** Défaillance technique inattendue côté serveur. */
	INTERNAL_ERROR
}
