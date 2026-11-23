package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Invitations à une fiche (BACK-74).
 */
public interface BackChildInvitationJpaRepository extends JpaRepository<BackChildInvitationEntity, Long> {

	boolean existsByChildIdAndEmailAndStatus(long childId, String email, String status);

	List<BackChildInvitationEntity> findByEmailAndStatus(String email, String status);

	Optional<BackChildInvitationEntity> findByIdAndStatus(long id, String status);

	List<BackChildInvitationEntity> findByChildIdAndStatus(long childId, String status);
}
