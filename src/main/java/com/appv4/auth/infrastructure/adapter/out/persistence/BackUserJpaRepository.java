package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Accès JPA à {@code back_users}.
 *
 * <p>{@code findByEmail} servira au login (1 email = 1 user) et aux contrôles
 * d'unicité signup. Spring Data génère la requête depuis le nom de méthode.
 */
public interface BackUserJpaRepository extends JpaRepository<BackUserEntity, Long> {

	Optional<BackUserEntity> findByEmail(String email);

	/**
	 * Claim atomique du premier choix de rôles (issue #34).
	 *
	 * @return 1 si transition {@code false → true}, 0 si déjà complété
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update BackUserEntity u
			set u.initialRolesCompleted = true, u.updatedAt = :now
			where u.id = :userId and u.initialRolesCompleted = false
			""")
	int claimInitialRolesCompleted(@Param("userId") long userId, @Param("now") Instant now);
}
