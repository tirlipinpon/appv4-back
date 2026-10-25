package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Liens entre enfants d'un même parent (SCRUM-158).
 */
public interface BackChildLinkJpaRepository extends JpaRepository<BackChildLinkEntity, Long> {

	@Query("""
			select link from BackChildLinkEntity link
			join fetch link.subject
			join fetch link.related
			where link.parentUserId = :parentUserId
			order by link.id
			""")
	List<BackChildLinkEntity> findDetailedByParentUserId(@Param("parentUserId") long parentUserId);
}
