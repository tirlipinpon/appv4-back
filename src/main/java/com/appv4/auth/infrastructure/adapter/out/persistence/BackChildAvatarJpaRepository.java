package com.appv4.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA {@code back_child_avatars} (BACK-66).
 */
public interface BackChildAvatarJpaRepository extends JpaRepository<BackChildAvatarEntity, Long> {
}
