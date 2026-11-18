package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BackSubjectLessonJpaRepository extends JpaRepository<BackSubjectLessonEntity, Long> {

	List<BackSubjectLessonEntity> findByTopicIdInOrderBySortOrderAscIdAsc(Collection<Long> topicIds);
}
