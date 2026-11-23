package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Années cochées pour les écoles d'un professeur (SCRUM-159).
 */
public interface BackTeacherSchoolGradeJpaRepository extends JpaRepository<BackTeacherSchoolGradeEntity, Long> {

	List<BackTeacherSchoolGradeEntity> findByTeacherSchoolIdInOrderByIdAsc(Collection<Long> teacherSchoolIds);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from BackTeacherSchoolGradeEntity grade where grade.teacherSchoolId in :ids")
	void deleteByTeacherSchoolIdIn(@Param("ids") Collection<Long> ids);
}
