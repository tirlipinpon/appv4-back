package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import com.appv4.auth.application.service.question.play.PlayableQuestionForm;

/** Champs métier des variantes HTTP, indépendants des métadonnées communes. */
record QuestionHttpForm(
		String playContent, String answerField, List<String> teacherRequired, List<String> teacherOptional) {
	static QuestionHttpForm of(PlayableQuestionForm form) {
		return switch (form) {
			case QCM -> new QuestionHttpForm("PlayQcm", "selectedChoiceIds", List.of("prompt", "choices"), List.of());
			case TRUE_FALSE -> new QuestionHttpForm("PlayTrueFalse", "statements", List.of("statements"), List.of());
			case MEMORY -> new QuestionHttpForm("PlayMemory", "memoryMatches", List.of("pairs"), List.of());
			case ORDER -> new QuestionHttpForm("PlayOrder", "orderedItemIds", List.of("items"), List.of("timelineEnabled"));
			case CLASSIFICATION -> new QuestionHttpForm(
					"PlayClassification", "placements", List.of("columns", "cards"), List.of());
			case LINKS -> new QuestionHttpForm("PlayLinks", "linkSelections", List.of("links"), List.of());
			case GAPS -> new QuestionHttpForm("PlayGaps", "gapFills", List.of("sentence", "answers"), List.of("decoys"));
			case CLICK_IMAGE -> new QuestionHttpForm("PlayImage", "zoneClicks", List.of("imageWebp", "zones"),
					List.of("clicksOrdered", "requireAllCorrect"));
			case LETTERS -> new QuestionHttpForm("PlayLetters", "attempt", List.of("prompt", "expectedAnswer"), List.of());
			case TEXT -> new QuestionHttpForm("PlayText", "attempt", List.of("prompt", "expectedAnswer"), List.of());
			case PUZZLE -> new QuestionHttpForm("PlayPuzzle", "puzzlePlacements", List.of("imageWebp", "pieces"),
					List.of("boardMode", "boardImageWebp", "snapDistance"));
			case SIMON -> new QuestionHttpForm("PlaySimon", "simonSequence", List.of("elementType", "elements"),
					List.of("sequence"));
		};
	}

	String legacyAnswerField() {
		return switch (answerField) {
			case "memoryMatches" -> "matchedPairIds";
			case "linkSelections" -> "linkMatches";
			default -> answerField;
		};
	}
}
