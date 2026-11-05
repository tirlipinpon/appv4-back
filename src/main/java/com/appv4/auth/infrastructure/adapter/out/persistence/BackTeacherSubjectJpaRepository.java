package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Matières enregistrées pour un professeur (SCRUM-161).
 */
public interface BackTeacherSubjectJpaRepository extends JpaRepository<BackTeacherSubjectEntity, Long> {

	List<BackTeacherSubjectEntity> findByUserIdOrderByIdAsc(long userId);

	List<BackTeacherSubjectEntity> findBySchoolIdInAndGradeLabelInAndHiddenFalseOrderByIdAsc(
			List<Long> schoolIds, List<String> grades);

	List<BackTeacherSubjectEntity> findBySchoolIdAndGradeLabelOrderByIdAsc(long schoolId, String gradeLabel);

	Optional<BackTeacherSubjectEntity> findFirstBySchoolIdAndGradeLabelAndNameKeyAndHiddenFalseOrderByIdAsc(
			long schoolId, String gradeLabel, String nameKey);

	@Query("""
			select s from BackTeacherSubjectEntity s
			where s.schoolId = :schoolId and s.gradeLabel = :grade and s.nameKey = :nameKey
			  and (s.hidden = true or not exists (
			    select a.id from BackTeacherSubjectAccessEntity a where a.subjectId = s.id))
			order by s.id desc
			""")
	List<BackTeacherSubjectEntity> findRemovedByName(@Param("schoolId") long schoolId,
			@Param("grade") String grade, @Param("nameKey") String nameKey, Pageable page);

	List<BackTeacherSubjectEntity> findByHiddenFalseAndActiveTrueOrderByIdAsc();

	Optional<BackTeacherSubjectEntity> findByIdAndUserId(long id, long userId);
}
