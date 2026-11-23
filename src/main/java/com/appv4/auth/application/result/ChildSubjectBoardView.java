package com.appv4.auth.application.result;

import java.util.List;

/**
 * Détail board d'une matière pour Learn (BACK-33).
 *
 * <p><b>Business :</b> hero, progression, Continuer et thèmes viennent du back.
 * <p><b>Technique :</b> record métier avant {@code GET /api/ext/subjects/{id}}.
 * <p><b>Métier / flow :</b> {@code ReadChildSubjectBoardUseCase} → controller.
 */
public record ChildSubjectBoardView(
		long subjectId,
		String name,
		String iconCode,
		String grade,
		String description,
		String tagline,
		String worldLabel,
		String ageLabel,
		String levelLabel,
		int xpEarned,
		int badgeCount,
		int lessonsCompleted,
		int lessonsTotal,
		Integer minutesLeft,
		ContinueLesson continueLesson,
		List<TopicCard> topics,
		ChildLessonView lesson) {

	public ChildSubjectBoardView {
		topics = topics == null ? List.of() : List.copyOf(topics);
		lesson = lesson == null ? new ChildLessonView(0, List.of()) : lesson;
	}

	public record ContinueLesson(
			long lessonId,
			String title,
			int progressPercent,
			int minutesLeft,
			int rewardXp,
			long playQuestionId) {
	}

	public record TopicCard(
			long topicId,
			String name,
			String iconCode,
			int lessonsCompleted,
			int lessonsTotal) {
	}
}
