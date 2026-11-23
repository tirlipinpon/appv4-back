package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Liens entre enfants d'un même parent (SCRUM-158).
 */
public interface BackChildLinkJpaRepository extends JpaRepository<BackChildLinkEntity, Long> {

	/**
	 * Prénom et sexe seulement. La photo reste sur {@code GET …/photo}.
	 */
	@Query("""
			select subject.id as subjectId, subject.firstName as subjectFirstName, subject.sex as subjectSex,
			       related.id as relatedId, related.firstName as relatedFirstName, related.sex as relatedSex,
			       link.relation as relation
			from BackChildLinkEntity link
			join link.subject subject
			join link.related related
			where link.parentUserId = :parentUserId
			order by link.id
			""")
	List<ChildLinkFace> findFacesByParentUserId(@Param("parentUserId") long parentUserId);

	interface ChildLinkFace {
		Long getSubjectId();

		String getSubjectFirstName();

		String getSubjectSex();

		Long getRelatedId();

		String getRelatedFirstName();

		String getRelatedSex();

		String getRelation();
	}

	/** Paire déjà enregistrée, quel que soit l'enfant qui a été créé en second. */
	@Query("""
			select link from BackChildLinkEntity link
			where link.parentUserId = :parentUserId
			and ((link.subject.id = :leftId and link.related.id = :rightId)
				or (link.subject.id = :rightId and link.related.id = :leftId))
			""")
	Optional<BackChildLinkEntity> findPair(
			@Param("parentUserId") long parentUserId,
			@Param("leftId") long leftId,
			@Param("rightId") long rightId);
}
