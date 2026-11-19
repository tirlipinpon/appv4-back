package com.appv4.auth.application.result;

import java.util.List;

/**
 * Question jouable pour l'enfant (BACK-16) — sans la solution.
 *
 * <p><b>Business :</b> l'enfant répond sans voir le vrai/faux ni les bons choix.
 * Memory : les deux faces d'une paire sont le contenu jouable (pas une solution cachée).
 * Ordre : les items sont renvoyés mélangés ; l'ordre professeur reste côté serveur.
 * Si {@code timelineEnabled}, chaque item porte son {@code axisLabel} de jalon.
 * Classement : colonnes et cartes sans la colonne juste.
 * Liens : les deux libellés d'un lien sont le contenu jouable ; l'association est notée à la tentative.
 * Case vide : la phrase et la banque (mots justes + leurres) sont le contenu jouable ;
 * quel mot va dans quel trou est noté à la tentative. Les mots sont mélangés.
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
		List<PlayPair> pairs,
		List<PlayItem> items,
		List<PlayColumn> columns,
		List<PlayCard> cards,
		Boolean timelineEnabled,
		List<PlayLink> links,
		String sentence,
		List<PlayGapWord> words) {

	public ChildPlayQuestionView {
		choices = choices == null ? List.of() : List.copyOf(choices);
		statements = statements == null ? List.of() : List.copyOf(statements);
		pairs = pairs == null ? List.of() : List.copyOf(pairs);
		items = items == null ? List.of() : List.copyOf(items);
		columns = columns == null ? List.of() : List.copyOf(columns);
		cards = cards == null ? List.of() : List.copyOf(cards);
		links = links == null ? List.of() : List.copyOf(links);
		words = words == null ? List.of() : List.copyOf(words);
		prompt = prompt == null ? "" : prompt;
		sentence = sentence == null ? "" : sentence;
	}

	public record PlayChoice(long choiceId, String label) {
	}

	public record PlayStatement(long statementId, String label) {
	}

	public record PlayPair(long pairId, String questionSide, String answerSide) {
	}

	public record PlayItem(long itemId, String label, String axisLabel) {
		public PlayItem(long itemId, String label) {
			this(itemId, label, null);
		}
	}

	public record PlayColumn(long columnId, String label) {
	}

	public record PlayCard(long cardId, String label) {
	}

	/** Deux côtés d'un lien. {@code linkId} est le même pour le mot et sa réponse. */
	public record PlayLink(long linkId, String leftLabel, String rightLabel) {
	}

	/** Mot de la banque Case vide. L'id ne dit pas si le mot est un leurre. */
	public record PlayGapWord(long wordId, String label) {
	}
}
