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
 * Image à cliquer : l'image et la géométrie des zones, sans dire lesquelles sont justes.
 * Si les clics sont ordonnés, chaque étape porte la consigne (le nom de la zone).
 * Lettre par lettre : {@code letterCount} cases et {@code letterMask}
 * ({@code #} = lettre, espace = saut), sans le mot attendu.
 * Puzzle : image de découpe, mode de plateau, fond optionnel, magnétisme et géométrie
 * des pièces (y compris slots blancs) ; l'enfant replace les pièces, le serveur note.
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
		List<PlayGapWord> words,
		String imageWebp,
		Boolean clicksOrdered,
		List<PlayClickZone> clickZones,
		List<PlayClickStep> clickSteps,
		Integer letterCount,
		String letterMask,
		String boardMode,
		String boardImageWebp,
		Double snapDistance,
		List<PlayPuzzlePiece> puzzlePieces) {

	public ChildPlayQuestionView(
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
		this(questionId, subjectId, formCode, prompt, choices, statements, pairs, items, columns, cards,
				timelineEnabled, links, sentence, words, "", null, List.of(), List.of(), null, null,
				null, "", null, List.of());
	}

	public ChildPlayQuestionView(
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
			List<PlayGapWord> words,
			String imageWebp,
			Boolean clicksOrdered,
			List<PlayClickZone> clickZones,
			List<PlayClickStep> clickSteps) {
		this(questionId, subjectId, formCode, prompt, choices, statements, pairs, items, columns, cards,
				timelineEnabled, links, sentence, words, imageWebp, clicksOrdered, clickZones, clickSteps,
				null, null, null, "", null, List.of());
	}

	public ChildPlayQuestionView(
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
			List<PlayGapWord> words,
			String imageWebp,
			Boolean clicksOrdered,
			List<PlayClickZone> clickZones,
			List<PlayClickStep> clickSteps,
			Integer letterCount,
			String letterMask) {
		this(questionId, subjectId, formCode, prompt, choices, statements, pairs, items, columns, cards,
				timelineEnabled, links, sentence, words, imageWebp, clicksOrdered, clickZones, clickSteps,
				letterCount, letterMask, null, "", null, List.of());
	}

	public ChildPlayQuestionView {
		choices = choices == null ? List.of() : List.copyOf(choices);
		statements = statements == null ? List.of() : List.copyOf(statements);
		pairs = pairs == null ? List.of() : List.copyOf(pairs);
		items = items == null ? List.of() : List.copyOf(items);
		columns = columns == null ? List.of() : List.copyOf(columns);
		cards = cards == null ? List.of() : List.copyOf(cards);
		links = links == null ? List.of() : List.copyOf(links);
		words = words == null ? List.of() : List.copyOf(words);
		clickZones = clickZones == null ? List.of() : List.copyOf(clickZones);
		clickSteps = clickSteps == null ? List.of() : List.copyOf(clickSteps);
		puzzlePieces = puzzlePieces == null ? List.of() : List.copyOf(puzzlePieces);
		prompt = prompt == null ? "" : prompt;
		sentence = sentence == null ? "" : sentence;
		imageWebp = imageWebp == null ? "" : imageWebp;
		letterMask = letterMask == null ? "" : letterMask;
		boardImageWebp = boardImageWebp == null ? "" : boardImageWebp;
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

	/** Géométrie d'une zone, sans dire si elle est juste. Les points sont entre 0 et 1. */
	public record PlayClickZone(long zoneId, String shape, List<PlayClickPoint> points) {
		public PlayClickZone {
			points = points == null ? List.of() : List.copyOf(points);
		}
	}

	public record PlayClickPoint(double x, double y) {
	}

	/** Consigne d'une étape, dans l'ordre, quand les clics sont ordonnés. */
	public record PlayClickStep(long zoneId, String prompt) {
	}

	/**
	 * Pièce jouable. {@code slotX}/{@code slotY} : cible posée par le professeur (cadre blanc,
	 * fond dédié), sinon null. {@code slotWidth} : largeur de la pièce sur le fond (proportion du
	 * plateau), null = taille de la découpe. Les points décrivent la découpe (cadre d'origine
	 * quand il n'y a pas de place posée).
	 */
	public record PlayPuzzlePiece(
			long pieceId,
			String label,
			String shape,
			List<PlayClickPoint> points,
			Double slotX,
			Double slotY,
			Double slotWidth) {
		public PlayPuzzlePiece {
			points = points == null ? List.of() : List.copyOf(points);
			label = label == null ? "" : label;
			shape = shape == null ? "" : shape;
		}
	}
}
