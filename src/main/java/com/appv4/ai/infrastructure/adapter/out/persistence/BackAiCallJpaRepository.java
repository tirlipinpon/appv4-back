package com.appv4.ai.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BackAiCallJpaRepository extends JpaRepository<BackAiCallEntity, Long> {

	List<BackAiCallEntity> findAllByOrderByIdAsc();
}
