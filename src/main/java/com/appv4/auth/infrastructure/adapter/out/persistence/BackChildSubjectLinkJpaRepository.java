package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Liens matière d'un enfant (SCRUM-162).
 */
public interface BackChildSubjectLinkJpaRepository extends JpaRepository<BackChildSubjectLinkEntity, Long> {

	List<BackChildSubjectLinkEntity> findByChildIdOrderByIdAsc(long childId);

	List<BackChildSubjectLinkEntity> findBySubjectId(long subjectId);

	Optional<BackChildSubjectLinkEntity> findByChildIdAndSubjectId(long childId, long subjectId);

	@Query("""
			select link from BackChildSubjectLinkEntity link
			where link.childId in (
				select child.id from BackChildEntity child
				where child.parentUserId = :parentUserId and child.hidden = false)
			""")
	List<BackChildSubjectLinkEntity> findForParent(@Param("parentUserId") long parentUserId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			delete from BackChildSubjectLinkEntity link
			where link.childId = :childId and link.subjectId = :subjectId
			""")
	int deleteLink(@Param("childId") long childId, @Param("subjectId") long subjectId);
}
