package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Contenu board d'une matière (BACK-33).
 *
 * <p><b>Business :</b> thèmes et leçons préparés pour Learn, sans inventer côté front.
 * <p><b>Technique :</b> tables {@code back_subject_board}, topics, lessons, lesson_questions.
 * <p><b>Métier / flow :</b> lecture board → ce port ; progression via attempts.
 */
public interface ChildSubjectBoardPort {

	Optional<BoardMeta> findMeta(long subjectId);

	List<StoredTopic> topicsOf(long subjectId);

	List<StoredLesson> lessonsOfTopics(List<Long> topicIds);

	List<LessonQuestion> questionsOfLessons(List<Long> lessonIds);

	/** Questions de la matière pour lesquelles l'enfant a au moins une réussite. */
	Set<Long> passedQuestionIds(long childId, long subjectId);

	/** Somme des gains XP des tentatives de cet enfant sur la matière. */
	int xpEarned(long childId, long subjectId);

	record BoardMeta(
			String description,
			String tagline,
			String worldLabel,
			String ageLabel,
			String levelLabel) {
	}

	record StoredTopic(long id, long subjectId, String name, String iconCode, int sortOrder) {
	}

	record StoredLesson(
			long id,
			long topicId,
			String title,
			int sortOrder,
			int estimatedMinutes,
			int rewardXp) {
	}

	record LessonQuestion(long lessonId, long questionId, int sortOrder) {
	}
}
