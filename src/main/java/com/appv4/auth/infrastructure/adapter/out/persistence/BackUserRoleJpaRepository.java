package com.appv4.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BackUserRoleJpaRepository extends JpaRepository<BackUserRoleEntity, Long> {

	boolean existsByUserIdAndRole(long userId, String role);
}
