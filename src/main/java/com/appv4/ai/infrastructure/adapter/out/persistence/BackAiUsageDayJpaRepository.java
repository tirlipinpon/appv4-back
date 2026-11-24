package com.appv4.ai.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BackAiUsageDayJpaRepository extends JpaRepository<BackAiUsageDayEntity, UsageDayKey> {
}
