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

	/** Serialize progress writers while allowing foreign-key references from concurrent attempts. */
	@Query(value = "select id from back_children where id = :childId for no key update", nativeQuery = true)
	long lockForProgressUpdate(@Param("childId") long childId);

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

	interface ChildSummary extends StudentSummary {
		long getParentUserId();
		String getSex();
		boolean getActive();
	}

	@Query("""
			select c.id as id, c.parentUserId as parentUserId, c.firstName as firstName,
			c.lastName as lastName, c.sex as sex, c.age as age, c.curriculum as curriculum,
			c.gradeLabel as gradeLabel, c.schoolId as schoolId, c.avatarCode as avatarCode,
			(case when c.photo is null then false else true end) as hasPhoto, c.active as active
			from BackChildEntity c where c.parentUserId = :parentUserId and c.hidden = false order by c.id
			""")
	List<ChildSummary> findParentSummaries(@Param("parentUserId") long parentUserId);

	Optional<BackChildEntity> findByIdAndParentUserIdAndHiddenFalse(long id, long parentUserId);

	Optional<BackChildEntity> findByIdAndParentUserId(long id, long parentUserId);

	/** Fiches encore proposées dans cette école et cette année. */
	List<BackChildEntity> findByHiddenFalseAndActiveTrueAndSchoolIdAndGradeLabel(long schoolId, String gradeLabel);

	/** Fiche encore proposée, et qui a une école. Le PIN n'est pas lu par cette requête. */
	Optional<BackChildEntity> findByIdAndHiddenFalseAndActiveTrueAndSchoolIdIsNotNull(long id);

	/**
	 * Fiches encore jouables dont le prénom, une fois coupé et mis en minuscules, est celui saisi.
	 * Le hash sert au login enfant. Une fiche masquée ou désactivée n'est pas proposée.
	 */
	@Query("""
			select c.id as id, c.firstName as firstName, c.pinHash as pinHash
			from BackChildEntity c
			where c.hidden = false and c.active = true
			and lower(trim(c.firstName)) = :firstName
			""")
	List<ChildLoginRow> findActiveLoginByFirstName(@Param("firstName") String firstName);

	/** Même lecture, par id, pour vérifier qu'une session enfant vise encore une fiche jouable. */
	@Query("""
			select c.id as id, c.firstName as firstName, c.pinHash as pinHash
			from BackChildEntity c
			where c.id = :id and c.hidden = false and c.active = true
			""")
	Optional<ChildLoginRow> findActiveLoginById(@Param("id") long id);

	@Query("""
			select c.id as id, c.firstName as firstName, c.avatarCode as avatarCode,
			(case when c.photo is null then false else true end) as hasPhoto,
			c.age as age
			from BackChildEntity c
			where c.id = :id and c.hidden = false and c.active = true
			""")
	Optional<ChildProfileRow> findActiveProfileById(@Param("id") long id);

	interface ChildLoginRow {
		long getId();
		String getFirstName();
		String getPinHash();
	}

	interface ChildProfileRow {
		long getId();
		String getFirstName();
		String getAvatarCode();
		boolean getHasPhoto();
		int getAge();
	}
}
