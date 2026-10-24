package com.appv4.auth.application;

/**
 * Messages publics auth partagés (anti-énumération, conflits).
 */
public final class AuthMessages {

	/** Collision email / identité — signup et contrainte SQL concurrente (issues #45, #44). */
	public static final String ACCOUNT_CONFLICT = "Impossible de créer le compte";

	private AuthMessages() {
	}
}
