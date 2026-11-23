package com.appv4.auth.application.result;

import java.util.List;

/**
 * Mission du jour prête à afficher (BACK-18).
 *
 * <p><b>Business :</b> l'accueil montre l'objectif, la progression et le lien vers
 * la prochaine activité. {@code mission} vide = rien à jouer aujourd'hui.
 * <p><b>Technique :</b> record métier avant le JSON {@code GET /api/ext/today}.
 * <p><b>Métier / flow :</b> {@code ReadChildTodayUseCase} → controller.
 */
public record ChildTodayView(Mission mission) {

	public static ChildTodayView empty() {
		return new ChildTodayView(null);
	}

	public record Mission(
			long id,
			String objective,
			int progress,
			int target,
			int rewardXp,
			String status,
			String statusLabel,
			long secondsRemaining,
			Long playSubjectId,
			Long playQuestionId,
			boolean playable,
			List<Task> tasks) {
		public Mission {
			tasks = tasks == null ? List.of() : List.copyOf(tasks);
		}
	}

	public record Task(
			long id,
			String label,
			int progress,
			int target,
			int rewardXp,
			String status,
			String statusLabel,
			long subjectId,
			long questionId,
			boolean playable) {
	}
}
