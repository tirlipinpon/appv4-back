package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BackTeacherRoleRequestJpaRepository extends JpaRepository<BackTeacherRoleRequestEntity, Long> {

	boolean existsByUserIdAndStatus(long userId, String status);

	List<BackTeacherRoleRequestEntity> findByStatusIn(Collection<String> statuses, Pageable pageable);
}
