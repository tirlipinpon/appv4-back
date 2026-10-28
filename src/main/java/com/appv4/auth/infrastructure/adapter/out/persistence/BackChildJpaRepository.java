package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Enfants d'un parent (SCRUM-156).
 */
public interface BackChildJpaRepository extends JpaRepository<BackChildEntity, Long> {

	List<BackChildEntity> findByParentUserIdAndHiddenFalseOrderByIdAsc(long parentUserId);

	Optional<BackChildEntity> findByIdAndParentUserIdAndHiddenFalse(long id, long parentUserId);

	Optional<BackChildEntity> findByIdAndParentUserId(long id, long parentUserId);

	/**
	 * Enfants encore proposés dans une de ces écoles (SCRUM-160).
	 * Les fiches masquées ou désactivées ne sortent pas : elles ne sont plus dans le reste de l'application.
	 */
	List<BackChildEntity> findByHiddenFalseAndActiveTrueAndSchoolIdIn(Collection<Long> schoolIds);

	/** Fiches encore proposées dans cette école et cette année. */
	List<BackChildEntity> findByHiddenFalseAndActiveTrueAndSchoolIdAndGradeLabel(long schoolId, String gradeLabel);

	/** Fiche encore proposée, et qui a une école. Le PIN n'est pas lu par cette requête. */
	Optional<BackChildEntity> findByIdAndHiddenFalseAndActiveTrueAndSchoolIdIsNotNull(long id);
}
