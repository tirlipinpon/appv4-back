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
				where child.hidden = false and (
					child.parentUserId = :parentUserId
					or exists (
						select holder.childId from BackChildHolderEntity holder
						where holder.childId = child.id and holder.userId = :parentUserId)))
			""")
	List<BackChildSubjectLinkEntity> findForParent(@Param("parentUserId") long parentUserId);

	@Modifying(flushAutomatically = true)
	@Query(value = "insert into back_child_subject_links (child_id, subject_id, excluded) "
			+ "values (:childId, :subjectId, false) on conflict (child_id, subject_id) do nothing", nativeQuery = true)
	int insertDefaultIfAbsent(@Param("childId") long childId, @Param("subjectId") long subjectId);

	/**
	 * Une passe pour toute la famille. Le conflit laisse une exclusion déjà posée.
	 */
	@Modifying(flushAutomatically = true)
	@Query(value = """
			insert into back_child_subject_links (child_id, subject_id, excluded)
			select c.id, s.id, false
			from back_children c
			join back_teacher_subjects s
			  on s.school_id = c.school_id
			 and s.grade_label = c.grade_label
			 and s.hidden = false
			 and s.active = true
			where c.hidden = false
			  and c.school_id is not null
			  and btrim(c.grade_label) <> ''
			  and (
			    c.parent_user_id = :parentUserId
			    or exists (
			      select 1 from back_child_holders holder
			      where holder.child_id = c.id and holder.user_id = :parentUserId))
			on conflict (child_id, subject_id) do nothing
			""", nativeQuery = true)
	int insertDefaultsForParent(@Param("parentUserId") long parentUserId);

	/**
	 * Une passe pour un enfant (filet learning). Le conflit laisse une exclusion déjà posée.
	 */
	@Modifying(flushAutomatically = true)
	@Query(value = """
			insert into back_child_subject_links (child_id, subject_id, excluded)
			select c.id, s.id, false
			from back_children c
			join back_teacher_subjects s
			  on s.school_id = c.school_id
			 and s.grade_label = c.grade_label
			 and s.hidden = false
			 and s.active = true
			where c.id = :childId
			  and c.hidden = false
			  and c.school_id is not null
			  and btrim(c.grade_label) <> ''
			on conflict (child_id, subject_id) do nothing
			""", nativeQuery = true)
	int insertDefaultsForChild(@Param("childId") long childId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			delete from BackChildSubjectLinkEntity link
			where link.childId = :childId and link.subjectId = :subjectId
			""")
	int deleteLink(@Param("childId") long childId, @Param("subjectId") long subjectId);
}
