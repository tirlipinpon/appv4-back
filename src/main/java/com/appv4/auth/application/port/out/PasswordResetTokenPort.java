package com.appv4.auth.application.port.out;

import java.time.Instant;
import java.util.Optional;

/**
 * Port sortant — jetons reset mdp (SCRUM-108 / SCRUM-111).
 */
public interface PasswordResetTokenPort {

	/** Invalide tous les jetons encore non utilisés de ce user. */
	void invalidateUnusedForUser(long userId);

	/** Persiste un nouveau jeton (hash uniquement). */
	void create(long userId, String tokenHash, Instant expiresAt);

	/**
	 * Jeton valide : hash connu, {@code used_at} NULL, {@code expires_at} &gt; now.
	 */
	Optional<ValidResetToken> findValidByTokenHash(String tokenHash);

	/** Marque le jeton consommé ({@code used_at = now}). */
	void markUsed(long tokenId);

	record ValidResetToken(long tokenId, long userId) {
	}
}
