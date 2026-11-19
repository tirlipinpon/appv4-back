package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Éléments d'une question Ordre, dans l'ordre saisi (SCRUM-176). */
public interface BackSubjectQuestionItemJpaRepository extends JpaRepository<BackSubjectQuestionItemEntity, Long> {

	interface ItemWithOptions {
		long getQuestionId();
		Long getItemId();
		String getLabel();
		String getAxisLabel();
		boolean getTimelineEnabled();
	}

	@Query("""
			select q.id as questionId, i.id as itemId, i.label as label, i.axisLabel as axisLabel,
				coalesce(o.timelineEnabled, false) as timelineEnabled
			from BackSubjectQuestionEntity q
			left join BackSubjectQuestionItemEntity i on i.questionId = q.id
			left join BackSubjectQuestionOrderOptionsEntity o on o.questionId = q.id
			where q.id in :questionIds order by q.id, i.sortOrder, i.id
			""")
	List<ItemWithOptions> findItemsWithOptions(@Param("questionIds") List<Long> questionIds);

	/** Les anciens éléments partent avant d'écrire la correction. */
	@Modifying(flushAutomatically = true)
	@Query("delete from BackSubjectQuestionItemEntity row where row.questionId = :questionId")
	void deleteByQuestionId(@Param("questionId") long questionId);
}
