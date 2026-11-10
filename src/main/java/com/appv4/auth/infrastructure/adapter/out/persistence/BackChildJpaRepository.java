package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Enfants d'un parent (SCRUM-156).
 */
public interface BackChildJpaRepository extends JpaRepository<BackChildEntity, Long> {

	interface StudentSummary {
		Long getId();
		String getFirstName();
		String getLastName();
		short getAge();
		String getCurriculum();
		String getGradeLabel();
		Long getSchoolId();
		String getAvatarCode();
		boolean getHasPhoto();
	}

	@Query("""
			select c.id as id, c.firstName as firstName, c.lastName as lastName,
			c.age as age, c.curriculum as curriculum, c.gradeLabel as gradeLabel,
			c.schoolId as schoolId, c.avatarCode as avatarCode,
			(case when c.photo is null then false else true end) as hasPhoto
			from BackChildEntity c where c.hidden = false and c.active = true and c.schoolId in :schoolIds
			""")
	List<StudentSummary> findStudentSummaries(@Param("schoolIds") Collection<Long> schoolIds);

	List<BackChildEntity> findByParentUserIdAndHiddenFalseOrderByIdAsc(long parentUserId);

	Optional<BackChildEntity> findByIdAndParentUserIdAndHiddenFalse(long id, long parentUserId);

	Optional<BackChildEntity> findByIdAndParentUserId(long id, long parentUserId);

	/** Fiches encore proposées dans cette école et cette année. */
	List<BackChildEntity> findByHiddenFalseAndActiveTrueAndSchoolIdAndGradeLabel(long schoolId, String gradeLabel);

	/** Fiche encore proposée, et qui a une école. Le PIN n'est pas lu par cette requête. */
	Optional<BackChildEntity> findByIdAndHiddenFalseAndActiveTrueAndSchoolIdIsNotNull(long id);
}
