package com.appv4.auth.application.port.in;

import java.util.List;

import com.appv4.auth.application.result.ChildAttemptResultView;
import com.appv4.auth.application.result.ChildPlayQuestionView;

/**
 * Lire une question jouable et enregistrer une tentative (BACK-16).
 *
 * <p><b>Business :</b> jeton enfant seulement ; matière liée obligatoire.
 * <p><b>Technique :</b> GET détail sans solution + POST answers → score.
 * <p><b>Métier / flow :</b> Learn play → ce port → ports out questions / attempts.
 */
public interface ChildQuestionPlayUseCase {

	ChildPlayQuestionView question(String rawToken, long subjectId, long questionId);

	ChildAttemptResultView submit(
			String rawToken, long subjectId, long questionId, AttemptAnswers answers);

	record StatementAnswer(long statementId, boolean value) {
	}

	/** Placement d'une carte dans une colonne (classement). */
	record PlacementAnswer(long cardId, long columnId) {
	}

	/** Association choisie par l'enfant : mot de gauche vers réponse de droite. */
	record LinkMatchAnswer(long leftId, long rightId) {
	}

	/** Mot placé dans un trou. {@code holeIndex} suit les {@code ___} de gauche à droite. */
	record GapFillAnswer(int holeIndex, long wordId) {
	}

	/** Zone touchée. L'ordre de la liste est l'ordre des clics. */
	record ZoneClickAnswer(long zoneId) {
	}

	record AttemptAnswers(
			List<Long> selectedChoiceIds,
			List<StatementAnswer> statements,
			List<Long> matchedPairIds,
			List<Long> orderedItemIds,
			List<PlacementAnswer> placements,
			List<LinkMatchAnswer> linkMatches,
			List<GapFillAnswer> gapFills,
			List<ZoneClickAnswer> zoneClicks) {
		public AttemptAnswers(
				List<Long> selectedChoiceIds,
				List<StatementAnswer> statements,
				List<Long> matchedPairIds,
				List<Long> orderedItemIds,
				List<PlacementAnswer> placements,
				List<LinkMatchAnswer> linkMatches,
				List<GapFillAnswer> gapFills) {
			this(selectedChoiceIds, statements, matchedPairIds, orderedItemIds, placements, linkMatches, gapFills,
					List.of());
		}

		public AttemptAnswers {
			selectedChoiceIds = selectedChoiceIds == null ? List.of() : List.copyOf(selectedChoiceIds);
			statements = statements == null ? List.of() : List.copyOf(statements);
			matchedPairIds = matchedPairIds == null ? List.of() : List.copyOf(matchedPairIds);
			orderedItemIds = orderedItemIds == null ? List.of() : List.copyOf(orderedItemIds);
			placements = placements == null ? List.of() : List.copyOf(placements);
			linkMatches = linkMatches == null ? List.of() : List.copyOf(linkMatches);
			gapFills = gapFills == null ? List.of() : List.copyOf(gapFills);
			zoneClicks = zoneClicks == null ? List.of() : List.copyOf(zoneClicks);
		}
	}
}
