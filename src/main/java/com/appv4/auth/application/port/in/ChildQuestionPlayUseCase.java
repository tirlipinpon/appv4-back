package com.appv4.auth.application.port.in;

import java.util.List;
import java.util.UUID;

import com.appv4.auth.application.result.ChildAttemptResultView;
import com.appv4.auth.application.result.ChildLetterLocksView;
import com.appv4.auth.application.result.ChildPlayQuestionView;

/** Ouvrir, jouer et corriger une question dans l'espace enfant. */
public interface ChildQuestionPlayUseCase {

	ChildPlayQuestionView question(String rawToken, long subjectId, long questionId, boolean legacy);

	ChildAttemptResultView submit(String rawToken, long subjectId, long questionId, Submission submission);

	ChildLetterLocksView letterLocks(String rawToken, long subjectId, long questionId, String attempt);

	boolean memoryPair(String rawToken, long subjectId, long questionId, MemoryMatch match);

	record Submission(String declaredForm, Answer answer, UUID submissionId, UUID challengeId) {
	}

	sealed interface Answer permits Choices, Statements, LegacyMemory, MemoryMatches, Order, Placements,
			LegacyLinks, LinkSelections, GapFills, ImageClicks, Text, Puzzle, Simon {
	}

	record Choices(List<Long> ids) implements Answer {
	}

	record Statements(List<StatementAnswer> values) implements Answer {
	}

	record LegacyMemory(List<Long> pairIds) implements Answer {
	}

	record MemoryMatches(List<MemoryMatch> matches) implements Answer {
	}

	record Order(List<Long> ids) implements Answer {
	}

	record Placements(List<PlacementAnswer> values) implements Answer {
	}

	record LegacyLinks(List<LinkMatchAnswer> matches) implements Answer {
	}

	record LinkSelections(List<LinkSelection> matches) implements Answer {
	}

	record GapFills(List<GapFillAnswer> values) implements Answer {
	}

	record ImageClicks(List<ZoneClickAnswer> values) implements Answer {
	}

	record Text(String attempt) implements Answer {
	}

	record Puzzle(List<PuzzlePiecePlacementAnswer> values) implements Answer {
	}

	record Simon(List<Integer> sequence) implements Answer {
	}

	record StatementAnswer(long statementId, boolean value) {
	}

	record PlacementAnswer(long cardId, long columnId) {
	}

	record LinkMatchAnswer(long leftId, long rightId) {
	}

	record LinkSelection(UUID leftId, UUID rightId) {
	}

	record MemoryMatch(UUID firstCardId, UUID secondCardId) {
	}

	record GapFillAnswer(int holeIndex, long wordId) {
	}

	record ZoneClickAnswer(long zoneId) {
	}

	record PuzzlePiecePlacementAnswer(long pieceId, double x, double y) {
	}
}
