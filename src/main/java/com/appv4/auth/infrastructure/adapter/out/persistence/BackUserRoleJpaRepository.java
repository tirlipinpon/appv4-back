package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface BackUserRoleJpaRepository extends JpaRepository<BackUserRoleEntity, Long> {

	boolean existsByUserIdAndRole(long userId, String role);

	List<BackUserRoleEntity> findByUserId(long userId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	void deleteByUserIdAndRole(long userId, String role);
}
