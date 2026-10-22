package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Accès JPA à {@code back_password_reset_tokens}.
 */
public interface BackPasswordResetTokenJpaRepository extends JpaRepository<BackPasswordResetTokenEntity, Long> {

	@Query("select t from BackPasswordResetTokenEntity t where t.user.id = :userId and t.usedAt is null")
	List<BackPasswordResetTokenEntity> findUnusedByUserId(@Param("userId") Long userId);

	@Modifying(clearAutomatically = true)
	@Query("""
			update BackPasswordResetTokenEntity t
			set t.usedAt = CURRENT_TIMESTAMP
			where t.user.id = :userId and t.usedAt is null
			""")
	int markUnusedAsUsedForUser(@Param("userId") Long userId);
}
