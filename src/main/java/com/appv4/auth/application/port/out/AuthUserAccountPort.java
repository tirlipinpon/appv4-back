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
	 * Après liaison OAuth (email prouvé par le fournisseur) : retire le hash mdp local
	 * et invalide les sessions (issue #41 — anti prise de compte par pré-inscription).
	 */
	void clearPasswordAfterVerifiedSocialLink(long userId);

	/**
	 * Met à jour le hash mdp (SCRUM-111 renew) — ne touche pas email / display_name / identities.
	 * Incrémente aussi {@code security_version} pour invalider les JWT déjà émis (issue #49).
	 */
	void updatePasswordHash(long userId, String passwordHash);

	/**
	 * Invalide toutes les sessions JWT du compte (révocation de rôle, désactivation, etc.).
	 */
	void bumpSecurityVersion(long userId);

	/**
	 * État session pour émission / contrôle JWT (actif + version de sécurité).
	 */
	Optional<SessionState> findSessionState(long userId);

	/**
	 * Compte pour le nom affiché dans l'en-tête (parent, professeur ou admin).
	 */
	Optional<AuthUserAccount> findById(long userId);

	/**
	 * Avatar du bandeau. Vide si la personne n'en a pas choisi.
	 */
	default Optional<String> findAvatarCode(long userId) {
		return Optional.empty();
	}

	/**
	 * Compte actif pour login email/mdp (SCRUM-117) — inclut le hash (peut être null si social pur).
	 */
	Optional<AuthUserLoginCandidate> findLoginCandidateByEmail(String email);

	record AuthUserAccount(long id, String email, String displayName, boolean active) {
	}

	/**
	 * Snapshot minimal pour valider un JWT face à la base (issue #49).
	 *
	 * @param active           compte encore utilisable
	 * @param securityVersion  doit matcher le claim {@code sv} du jeton
	 */
	record SessionState(boolean active, int securityVersion) {
	}

	record AuthUserSignupState(
			long id,
			String email,
			boolean hasPassword,
			List<String> socialProviders) {
	}

	record AuthUserLoginCandidate(long id, String email, String passwordHash, boolean active) {
	}
}
