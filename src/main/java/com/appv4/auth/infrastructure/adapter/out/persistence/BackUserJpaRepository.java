package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data — {@code back_users}.
 */
public interface BackUserJpaRepository extends JpaRepository<BackUserEntity, Long> {

	Optional<BackUserEntity> findByEmail(String email);
}
