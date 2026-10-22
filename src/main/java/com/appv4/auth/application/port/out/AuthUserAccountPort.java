package com.appv4.auth.application.port.out;

import java.util.Optional;

/**
 * Port sortant — comptes admin ({@code back_users} + identities).
 */
public interface AuthUserAccountPort {

	Optional<AuthUserAccount> findByProviderSubject(String provider, String providerSubject);

	Optional<AuthUserAccount> findByEmail(String email);

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
	 * Lie un provider à un user déjà existant (même email).
	 */
	void linkIdentity(long userId, String provider, String providerSubject, String emailAtProvider);

	record AuthUserAccount(long id, String email, String displayName, boolean active) {
	}
}
