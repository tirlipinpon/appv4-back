package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BackChildDailyTaskJpaRepository extends JpaRepository<BackChildDailyTaskEntity, Long> {

	List<BackChildDailyTaskEntity> findByMissionIdOrderByPositionAsc(long missionId);
}
