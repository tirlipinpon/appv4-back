package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Demandes de partage (BACK-75).
 */
public interface BackChildShareRequestJpaRepository extends JpaRepository<BackChildShareRequestEntity, Long> {

	@Query("""
			select request from BackChildShareRequestEntity request
			where request.senderUserId = :senderId
			and request.referentUserId = :referentId
			and lower(request.firstName) = lower(:firstName)
			and lower(request.lastName) = lower(:lastName)
			and request.status = 'pending'
			""")
	Optional<BackChildShareRequestEntity> findPending(
			@Param("senderId") long senderId,
			@Param("referentId") long referentId,
			@Param("firstName") String firstName,
			@Param("lastName") String lastName);

	List<BackChildShareRequestEntity> findByReferentUserIdAndStatus(long referentUserId, String status);

	@Query("""
			select count(request) from BackChildShareRequestEntity request
			where request.referentUserId = :referentId
			and request.status = 'pending'
			and lower(request.firstName) = lower(:firstName)
			and lower(request.lastName) = lower(:lastName)
			""")
	int countPendingForName(
			@Param("referentId") long referentId,
			@Param("firstName") String firstName,
			@Param("lastName") String lastName);

	@Query("""
			select request from BackChildShareRequestEntity request
			where request.referentUserId = :referentId
			and request.status = 'pending'
			and lower(request.firstName) = lower(:firstName)
			and lower(request.lastName) = lower(:lastName)
			""")
	List<BackChildShareRequestEntity> findPendingForName(
			@Param("referentId") long referentId,
			@Param("firstName") String firstName,
			@Param("lastName") String lastName);
}
