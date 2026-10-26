package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Demandes de transfert de matière (SCRUM-189).
 */
public interface BackTeacherSubjectTransferRequestJpaRepository
		extends JpaRepository<BackTeacherSubjectTransferRequestEntity, Long> {

	List<BackTeacherSubjectTransferRequestEntity> findByToUserIdAndStatusOrderByIdAsc(
			long toUserId, String status);

	List<BackTeacherSubjectTransferRequestEntity> findBySubjectIdAndStatusOrderByIdAsc(
			long subjectId, String status);

	Optional<BackTeacherSubjectTransferRequestEntity> findByIdAndToUserIdAndStatus(
			long id, long toUserId, String status);

	boolean existsBySubjectIdAndToUserIdAndStatus(long subjectId, long toUserId, String status);
}
