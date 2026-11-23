package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Écoles rattachées à un professeur (SCRUM-159).
 */
public interface BackTeacherSchoolJpaRepository extends JpaRepository<BackTeacherSchoolEntity, Long> {

	List<BackTeacherSchoolEntity> findByUserIdOrderByIdAsc(long userId);

	@Query("""
			select distinct school.userId
			from BackTeacherSchoolEntity school, BackTeacherSchoolGradeEntity grade
			where school.id = grade.teacherSchoolId
			  and school.schoolId = :schoolId
			  and grade.gradeLabel = :grade
			""")
	List<Long> findUserIdsWithClass(@Param("schoolId") long schoolId, @Param("grade") String grade);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from BackTeacherSchoolEntity school where school.userId = :userId")
	void deleteByUserId(@Param("userId") long userId);
}
