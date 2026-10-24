package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Accès JPA à {@code back_password_reset_tokens}.
 */
public interface BackPasswordResetTokenJpaRepository extends JpaRepository<BackPasswordResetTokenEntity, Long> {

	@Query("select t from BackPasswordResetTokenEntity t where t.user.id = :userId and t.usedAt is null")
	List<BackPasswordResetTokenEntity> findUnusedByUserId(@Param("userId") Long userId);

	@Query("""
			select t from BackPasswordResetTokenEntity t
			where t.tokenHash = :tokenHash
			  and t.usedAt is null
			  and t.expiresAt > :now
			""")
	Optional<BackPasswordResetTokenEntity> findValidByTokenHash(
			@Param("tokenHash") String tokenHash,
			@Param("now") Instant now);

	@Modifying(clearAutomatically = true)
	@Query("""
			update BackPasswordResetTokenEntity t
			set t.usedAt = CURRENT_TIMESTAMP
			where t.user.id = :userId and t.usedAt is null
			""")
	int markUnusedAsUsedForUser(@Param("userId") Long userId);

	/** Consommation concurrente-safe : 1 = gagnant, 0 = déjà utilisé / inconnu. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update BackPasswordResetTokenEntity t
			set t.usedAt = :now
			where t.id = :tokenId and t.usedAt is null and t.expiresAt > :now
			""")
	int markUsedIfStillValid(@Param("tokenId") long tokenId, @Param("now") Instant now);
}
