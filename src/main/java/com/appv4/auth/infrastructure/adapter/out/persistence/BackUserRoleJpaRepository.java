package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BackUserRoleJpaRepository extends JpaRepository<BackUserRoleEntity, Long> {

	boolean existsByUserIdAndRole(long userId, String role);

	List<BackUserRoleEntity> findByUserId(long userId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	void deleteByUserIdAndRole(long userId, String role);

	/** Insert idempotent sous concurrence (issue #44) — pas de rollback-only. */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query(value = """
			INSERT INTO back_user_roles (user_id, role)
			VALUES (:userId, :role)
			ON CONFLICT (user_id, role) DO NOTHING
			""", nativeQuery = true)
	int insertIgnoreConflict(@Param("userId") long userId, @Param("role") String role);
}
