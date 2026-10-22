package com.appv4.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BackTeacherRoleRequestJpaRepository extends JpaRepository<BackTeacherRoleRequestEntity, Long> {

	boolean existsByUserIdAndStatus(long userId, String status);
}
