package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.ChildSubjectBoardPort;
import com.appv4.auth.application.service.ChildProgressRules;

/**
 * Persistance du board matière (BACK-33).
 *
 * <p><b>Business :</b> lit meta, thèmes, leçons et tentatives réussies.
 * <p><b>Technique :</b> JPA ; absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out du port {@link ChildSubjectBoardPort}.
 */
@Component
@Profile("!test")
public class ChildSubjectBoardPersistenceAdapter implements ChildSubjectBoardPort {

	private final BackSubjectBoardJpaRepository boards;
	private final BackSubjectTopicJpaRepository topics;
	private final BackSubjectLessonJpaRepository lessons;
	private final BackSubjectLessonQuestionJpaRepository lessonQuestions;
	private final BackChildAttemptJpaRepository attempts;

	public ChildSubjectBoardPersistenceAdapter(
			BackSubjectBoardJpaRepository boards,
			BackSubjectTopicJpaRepository topics,
			BackSubjectLessonJpaRepository lessons,
			BackSubjectLessonQuestionJpaRepository lessonQuestions,
			BackChildAttemptJpaRepository attempts) {
		this.boards = boards;
		this.topics = topics;
		this.lessons = lessons;
		this.lessonQuestions = lessonQuestions;
		this.attempts = attempts;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<BoardMeta> findMeta(long subjectId) {
		return boards.findById(subjectId).map(entity -> new BoardMeta(
				entity.getDescription(),
				entity.getTagline(),
				entity.getWorldLabel(),
				entity.getAgeLabel(),
				entity.getLevelLabel()));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredTopic> topicsOf(long subjectId) {
		return topics.findBySubjectIdOrderBySortOrderAscIdAsc(subjectId).stream()
				.map(entity -> new StoredTopic(
						entity.getId(),
						entity.getSubjectId(),
						entity.getName(),
						entity.getIconCode(),
						entity.getSortOrder()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredLesson> lessonsOfTopics(List<Long> topicIds) {
		if (topicIds == null || topicIds.isEmpty()) {
			return List.of();
		}
		return lessons.findByTopicIdInOrderBySortOrderAscIdAsc(topicIds).stream()
				.map(entity -> new StoredLesson(
						entity.getId(),
						entity.getTopicId(),
						entity.getTitle(),
						entity.getSortOrder(),
						entity.getEstimatedMinutes(),
						entity.getRewardXp()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<LessonQuestion> questionsOfLessons(List<Long> lessonIds) {
		if (lessonIds == null || lessonIds.isEmpty()) {
			return List.of();
		}
		return lessonQuestions.findByLessonIdInOrderBySortOrderAscQuestionIdAsc(lessonIds).stream()
				.map(entity -> new LessonQuestion(
						entity.getLessonId(), entity.getQuestionId(), entity.getSortOrder()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Set<Long> passedQuestionIds(long childId, long subjectId) {
		return new HashSet<>(attempts.findPassedQuestionIds(childId, subjectId));
	}

	@Override
	@Transactional(readOnly = true)
	public int xpEarned(long childId, long subjectId) {
		Long sum = attempts.sumScorePercent(childId, subjectId);
		if (sum == null || sum <= 0) {
			return 0;
		}
		return (int) (sum / ChildProgressRules.XP_DIVISOR);
	}
}
