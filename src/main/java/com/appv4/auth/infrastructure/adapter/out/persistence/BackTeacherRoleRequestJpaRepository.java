package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BackTeacherRoleRequestJpaRepository extends JpaRepository<BackTeacherRoleRequestEntity, Long> {

	boolean existsByUserIdAndStatus(long userId, String status);

	/** Tous les statuts de ce compte (pending, approved, revoked peuvent coexister). */
	@Query("select e.status from BackTeacherRoleRequestEntity e where e.userId = :userId")
	List<String> findStatusesByUserId(@Param("userId") long userId);

	List<BackTeacherRoleRequestEntity> findByStatusIn(Collection<String> statuses, Pageable pageable);

	/**
	 * Transition concurrente-safe : pending/revoked → approved.
	 *
	 * @return 1 si cette transaction gagne, 0 si déjà approved ou absente
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE BackTeacherRoleRequestEntity e
			SET e.status = 'approved', e.reviewedAt = :reviewedAt, e.revokedAt = null
			WHERE e.id = :id AND e.status IN ('pending', 'revoked')
			""")
	int approveFromOpen(@Param("id") long id, @Param("reviewedAt") Instant reviewedAt);

	/**
	 * Rafraîchit {@code reviewedAt} sur une demande déjà approved (réapprobation voulue).
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			UPDATE BackTeacherRoleRequestEntity e
			SET e.reviewedAt = :reviewedAt, e.revokedAt = null
			WHERE e.id = :id AND e.status = 'approved'
			""")
	int refreshApproved(@Param("id") long id, @Param("reviewedAt") Instant reviewedAt);

	/** Demande pending idempotente sous concurrence (issue #44). */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			INSERT INTO back_teacher_role_requests (user_id, status)
			SELECT :userId, 'pending'
			WHERE NOT EXISTS (
				SELECT 1 FROM back_teacher_role_requests r
				WHERE r.user_id = :userId AND r.status = 'pending'
			)
			""", nativeQuery = true)
	int insertPendingIfAbsent(@Param("userId") long userId);
}
