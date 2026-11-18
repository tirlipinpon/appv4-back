package com.appv4.auth.application.result;

/**
 * Verdict d'une tentative enfant (BACK-16).
 *
 * <p><b>Business :</b> le front affiche uniquement ce score / passed.
 * <p><b>Technique :</b> réponse POST attempts.
 * <p><b>Métier / flow :</b> grader → insert → cette vue.
 */
public record ChildAttemptResultView(
		long attemptId,
		int scorePercent,
		boolean passed) {
}
