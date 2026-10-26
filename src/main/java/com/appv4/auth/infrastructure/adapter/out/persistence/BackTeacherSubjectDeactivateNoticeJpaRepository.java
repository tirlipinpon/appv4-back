package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Alertes de désactivation matière (co-profs).
 */
public interface BackTeacherSubjectDeactivateNoticeJpaRepository
		extends JpaRepository<BackTeacherSubjectDeactivateNoticeEntity, Long> {

	List<BackTeacherSubjectDeactivateNoticeEntity> findByToUserIdAndDismissedAtIsNullOrderByIdAsc(long toUserId);

	Optional<BackTeacherSubjectDeactivateNoticeEntity> findByIdAndToUserIdAndDismissedAtIsNull(
			long id, long toUserId);
}
