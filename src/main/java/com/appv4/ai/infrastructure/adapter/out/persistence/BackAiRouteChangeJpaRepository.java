package com.appv4.ai.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BackAiRouteChangeJpaRepository extends JpaRepository<BackAiRouteChangeEntity, Long> {

	List<BackAiRouteChangeEntity> findAllByOrderByChangedAtDesc();
}
