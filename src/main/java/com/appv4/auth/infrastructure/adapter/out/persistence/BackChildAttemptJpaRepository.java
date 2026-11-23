package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BackChildAttemptJpaRepository extends JpaRepository<BackChildAttemptEntity, Long> {

	Optional<BackChildAttemptEntity> findByChildIdAndSubmissionId(long childId, UUID submissionId);

	@Query("""
			select distinct a.questionId from BackChildAttemptEntity a
			where a.childId = :childId and a.subjectId = :subjectId and a.passed = true
			""")
	List<Long> findPassedQuestionIds(@Param("childId") long childId, @Param("subjectId") long subjectId);

	@Query("""
			select coalesce(sum(a.scorePercent), 0) from BackChildAttemptEntity a
			where a.childId = :childId and a.subjectId = :subjectId
			""")
	Long sumScorePercent(@Param("childId") long childId, @Param("subjectId") long subjectId);
}
