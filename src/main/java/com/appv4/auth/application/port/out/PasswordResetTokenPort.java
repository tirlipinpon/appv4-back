package com.appv4.auth.application.port.out;

import java.time.Instant;

/**
 * Port sortant — jetons reset mdp (SCRUM-108).
 */
public interface PasswordResetTokenPort {

	/** Invalide tous les jetons encore non utilisés de ce user. */
	void invalidateUnusedForUser(long userId);

	/** Persiste un nouveau jeton (hash uniquement). */
	void create(long userId, String tokenHash, Instant expiresAt);
}
