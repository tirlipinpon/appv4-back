package com.appv4.auth.application.result;

/**
 * Verdict d'une tentative enfant.
 *
 * <p><b>Business :</b> le front affiche le score pédagogique, le rang dans la page,
 * les essais encore ouverts et les points du barème. Les points ne partent que si la réponse est juste.
 * L'XP de progression reste ce score divisé par 10. Les missions enregistrent le pourcentage et le verdict,
 * pas les points du barème. Rejouer reste sur la même page : {@code replayAvailable} dit si le budget le permet encore.
 * <p><b>Technique :</b> réponse POST attempts.
 * <p><b>Métier / flow :</b> grader → réserve de la page → insert → cette vue.
 */
public record ChildAttemptResultView(
		long attemptId,
		int scorePercent,
		boolean passed,
		int attemptRank,
		int attemptsRemaining,
		int pointsAwarded,
		boolean replayAvailable) {
}
