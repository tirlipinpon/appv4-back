package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Copies de centres d'intérêt (BACK-72).
 */
public interface BackChildInterestJpaRepository extends JpaRepository<BackChildInterestEntity, Long> {

	List<BackChildInterestEntity> findByChildId(long childId);

	void deleteByChildId(long childId);

	@Query("""
			select i.id as id, c.parentUserId as parentUserId, i.label as label, i.writtenAt as writtenAt,
			       c.active as active, c.hidden as hidden
			from BackChildInterestEntity i, BackChildEntity c
			where i.childId = c.id
			""")
	List<SuggestionRow> findWithChild();

	interface SuggestionRow {
		long getId();

		long getParentUserId();

		String getLabel();

		Instant getWrittenAt();

		boolean isActive();

		boolean isHidden();
	}
}
