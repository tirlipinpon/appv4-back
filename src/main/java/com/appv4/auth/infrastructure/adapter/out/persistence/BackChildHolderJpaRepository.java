package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Détenteurs d'une fiche (BACK-74).
 */
public interface BackChildHolderJpaRepository
		extends JpaRepository<BackChildHolderEntity, BackChildHolderEntity.Key> {

	Optional<BackChildHolderEntity> findByChildIdAndUserId(long childId, long userId);

	boolean existsByChildIdAndUserId(long childId, long userId);
}
