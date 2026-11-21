package com.appv4.auth.application.result;

/**
 * Verdict d'une tentative enfant.
 *
 * <p><b>Business :</b> le front affiche le score pédagogique, le rang dans la page,
 * les essais encore ouverts et les points du barème. Les points ne partent que si la réponse est juste.
 * L'XP de progression reste ce score divisé par 10, et seulement quand cet essai est payé.
 * Les missions enregistrent le pourcentage et le verdict, pas les points du barème.
 * Rejouer reste sur la même page : {@code replayAvailable} dit si le budget le permet encore.
 * {@code mark} est l'étoile après cet essai : {@code none}, {@code black} ou {@code yellow}.
 * {@code yellowCount} est le nombre d'étoiles jaunes, zéro quand il n'y en a pas.
 * <p><b>Technique :</b> réponse POST attempts. Une relance du même identifiant renvoie la même étoile.
 * <p><b>Métier / flow :</b> grader → réserve de la page → étoile → insert → cette vue.
 */
public record ChildAttemptResultView(
		long attemptId,
		int scorePercent,
		boolean passed,
		int attemptRank,
		int attemptsRemaining,
		int pointsAwarded,
		boolean replayAvailable,
		String mark,
		int yellowCount) {
}
