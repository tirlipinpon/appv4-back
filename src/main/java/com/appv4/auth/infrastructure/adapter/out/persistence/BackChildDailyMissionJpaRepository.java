package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BackChildDailyMissionJpaRepository extends JpaRepository<BackChildDailyMissionEntity, Long> {

	Optional<BackChildDailyMissionEntity> findByChildIdAndMissionDate(long childId, LocalDate missionDate);
}
