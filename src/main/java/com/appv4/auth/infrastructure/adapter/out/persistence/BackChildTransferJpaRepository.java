package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Transferts de fiche (BACK-76).
 */
public interface BackChildTransferJpaRepository extends JpaRepository<BackChildTransferEntity, Long> {

	List<BackChildTransferEntity> findByChildIdAndStatus(long childId, String status);

	@Query("""
			select transfer from BackChildTransferEntity transfer
			where transfer.status = 'pending'
			  and (transfer.fromUserId = :userId or transfer.toUserId = :userId)
			""")
	List<BackChildTransferEntity> findPendingForUser(@Param("userId") long userId);
}
