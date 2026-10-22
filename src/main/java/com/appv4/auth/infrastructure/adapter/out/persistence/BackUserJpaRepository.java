package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Accès JPA à {@code back_users}.
 *
 * <p>{@code findByEmail} servira au login (1 email = 1 user) et aux contrôles
 * d'unicité signup. Spring Data génère la requête depuis le nom de méthode.
 */
public interface BackUserJpaRepository extends JpaRepository<BackUserEntity, Long> {

	Optional<BackUserEntity> findByEmail(String email);
}
