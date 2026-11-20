package com.appv4.auth.application.result;

import java.util.List;
import java.util.UUID;

/** Projection jouable partagée entre l'enfant et la simulation professeur. */
public record ChildPlayQuestionView(
		long questionId, long subjectId, String formCode, String prompt, UUID challengeId, Payload content) {

	public sealed interface Payload permits PlayQcm, PlayTrueFalse, PlayMemory, PlayLegacyMemory, PlayOrder,
			PlayClassification, PlayLinks, PlayLegacyLinks, PlayGaps, PlayImage, PlayLetters, PlayText,
			PlayPuzzle, PlaySimon {
	}

	public record PlayQcm(List<PlayChoice> choices) implements Payload {
	}

	public record PlayTrueFalse(List<PlayStatement> statements) implements Payload {
	}

	public record PlayMemory(List<PlayMemoryCard> memoryCards) implements Payload {
	}

	public record PlayLegacyMemory(List<PlayPair> pairs) implements Payload {
	}

	public record PlayOrder(List<PlayItem> items, boolean timelineEnabled) implements Payload {
	}

	public record PlayClassification(List<PlayColumn> columns, List<PlayCard> cards) implements Payload {
	}

	public record PlayLinks(List<PlayLinkOption> linkLefts, List<PlayLinkOption> linkRights) implements Payload {
	}

	public record PlayLegacyLinks(List<PlayLink> links) implements Payload {
	}

	public record PlayGaps(String sentence, List<PlayGapWord> words) implements Payload {
	}

	public record PlayImage(String imageWebp, boolean clicksOrdered, List<PlayClickZone> clickZones,
			List<PlayClickStep> clickSteps) implements Payload {
	}

	public record PlayLetters(int letterCount, String letterMask) implements Payload {
	}

	public record PlayText() implements Payload {
	}

	public record PlayPuzzle(String imageWebp, String boardMode, String boardImageWebp, double snapDistance,
			List<PlayPuzzlePiece> puzzlePieces) implements Payload {
	}

	public record PlaySimon(String elementType, List<PlaySimonElement> elements, List<Integer> sequence)
			implements Payload {
	}

	public record PlayChoice(long choiceId, String label, String imageWebp) {
		public PlayChoice(long choiceId, String label) {
			this(choiceId, label, "");
		}
	}

	public record PlayStatement(long statementId, String label, String imageWebp) {
		public PlayStatement(long statementId, String label) {
			this(statementId, label, "");
		}
	}

	public record PlayPair(long pairId, String questionSide, String answerSide) {
	}

	public record PlayMemoryCard(UUID cardId, String label, String imageWebp) {
		public PlayMemoryCard(UUID cardId, String label) {
			this(cardId, label, "");
		}
	}

	public record PlayItem(long itemId, String label, String axisLabel, String imageWebp) {
		public PlayItem(long itemId, String label, String axisLabel) {
			this(itemId, label, axisLabel, "");
		}
	}

	public record PlayColumn(long columnId, String label) {
	}

	public record PlayCard(long cardId, String label, String imageWebp) {
		public PlayCard(long cardId, String label) {
			this(cardId, label, "");
		}
	}

	public record PlayLink(long linkId, String leftLabel, String rightLabel) {
	}

	public record PlayLinkOption(UUID optionId, String label, String imageWebp) {
		public PlayLinkOption(UUID optionId, String label) {
			this(optionId, label, "");
		}
	}

	public record PlayGapWord(long wordId, String label) {
	}

	public record PlayClickZone(long zoneId, String shape, List<PlayClickPoint> points) {
	}

	public record PlayClickPoint(double x, double y) {
	}

	public record PlayClickStep(long zoneId, String prompt) {
	}

	public record PlayPuzzlePiece(
			long pieceId, String label, String shape, List<PlayClickPoint> points,
			Double slotX, Double slotY, Double slotWidth) {
	}

	public record PlaySimonElement(String label, String color, String imageWebp) {
		public PlaySimonElement(String label, String color) {
			this(label, color, "");
		}
	}
}
