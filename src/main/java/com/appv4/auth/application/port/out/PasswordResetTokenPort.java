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

	/**
	 * Consomme le jeton de façon atomique (issue #43).
	 *
	 * @return {@code true} si cette transaction a gagné ; {@code false} si déjà utilisé
	 */
	boolean tryMarkUsed(long tokenId);

	record ValidResetToken(long tokenId, long userId) {
	}
}
