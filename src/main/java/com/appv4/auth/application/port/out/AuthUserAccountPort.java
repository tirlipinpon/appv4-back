package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Port sortant — comptes admin ({@code back_users} + identities).
 */
public interface AuthUserAccountPort {

	Optional<AuthUserAccount> findByProviderSubject(String provider, String providerSubject);

	Optional<AuthUserAccount> findByEmail(String email);

	/**
	 * Détail pour signup : password déjà posé ? providers sociaux liés ?
	 */
	Optional<AuthUserSignupState> findSignupStateByEmail(String email);

	/**
	 * Crée un user social (pas de password_hash) + ligne identity.
	 */
	AuthUserAccount createWithIdentity(
			String email,
			String displayName,
			String provider,
			String providerSubject,
			String emailAtProvider);

	/**
	 * Crée un user email/mdp (SCRUM-105) — {@code password_hash} renseigné.
	 */
	AuthUserAccount createWithPassword(String email, String displayName, String passwordHash);

	/**
	 * Lie un provider à un user déjà existant (même email).
	 */
	void linkIdentity(long userId, String provider, String providerSubject, String emailAtProvider);

	/**
	 * Met à jour le hash mdp (SCRUM-111 renew) — ne touche pas email / display_name / identities.
	 */
	void updatePasswordHash(long userId, String passwordHash);

	record AuthUserAccount(long id, String email, String displayName, boolean active) {
	}

	record AuthUserSignupState(
			long id,
			String email,
			boolean hasPassword,
			List<String> socialProviders) {
	}
}
