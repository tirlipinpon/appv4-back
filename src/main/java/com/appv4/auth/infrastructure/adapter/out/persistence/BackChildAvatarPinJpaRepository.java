package com.appv4.auth.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA {@code back_child_avatar_pins} (BACK-67).
 */
public interface BackChildAvatarPinJpaRepository extends JpaRepository<BackChildAvatarPinEntity, BackChildAvatarPinId> {

	void deleteByChildId(long childId);
}
