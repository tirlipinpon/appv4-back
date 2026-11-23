package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Genre des éléments des questions Simon, une ligne par question. */
public interface BackSubjectQuestionSimonOptionsJpaRepository
		extends JpaRepository<BackSubjectQuestionSimonOptionsEntity, Long> {

	List<BackSubjectQuestionSimonOptionsEntity> findByQuestionIdIn(Collection<Long> questionIds);
}
