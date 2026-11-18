package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BackSubjectTopicJpaRepository extends JpaRepository<BackSubjectTopicEntity, Long> {

	List<BackSubjectTopicEntity> findBySubjectIdOrderBySortOrderAscIdAsc(long subjectId);
}
