package com.appv4.auth.application.result;

import java.util.List;

/**
 * Question jouable pour l'enfant (BACK-16) — sans la solution.
 *
 * <p><b>Business :</b> l'enfant répond sans voir le vrai/faux ni les bons choix.
 * Memory : les deux faces d'une paire sont le contenu jouable (pas une solution cachée).
 * <p><b>Technique :</b> payload GET play.
 * <p><b>Métier / flow :</b> use case play → controller /api/ext.
 */
public record ChildPlayQuestionView(
		long questionId,
		long subjectId,
		String formCode,
		String prompt,
		List<PlayChoice> choices,
		List<PlayStatement> statements,
		List<PlayPair> pairs) {

	public ChildPlayQuestionView {
		choices = choices == null ? List.of() : List.copyOf(choices);
		statements = statements == null ? List.of() : List.copyOf(statements);
		pairs = pairs == null ? List.of() : List.copyOf(pairs);
		prompt = prompt == null ? "" : prompt;
	}

	public record PlayChoice(long choiceId, String label) {
	}

	public record PlayStatement(long statementId, String label) {
	}

	public record PlayPair(long pairId, String questionSide, String answerSide) {
	}
}
