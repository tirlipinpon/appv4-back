package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.Answer;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.MemoryMatch;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.Submission;
import com.appv4.auth.application.service.question.play.QuestionPlayEngine;

import io.swagger.v3.oas.annotations.media.Schema;

/** Une seule réponse est autorisée. Les anciens clients omettent formCode et submissionId. */
@Schema(name = "SubmitChildAttemptRequest")
public record PlayAttemptRequest(
		String formCode, UUID submissionId, UUID challengeId,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID playId,
		List<@NotNull @Positive Long> selectedChoiceIds,
		List<@NotNull @Valid SubmitStatementAnswer> statements,
		List<@NotNull @Positive Long> matchedPairIds,
		List<@NotNull @Valid SubmitMemoryMatch> memoryMatches,
		List<@NotNull @Positive Long> orderedItemIds,
		List<@NotNull @Valid SubmitPlacementAnswer> placements,
		List<@NotNull @Valid SubmitLinkMatchAnswer> linkMatches,
		List<@NotNull @Valid SubmitLinkSelection> linkSelections,
		List<@NotNull @Valid SubmitGapFillAnswer> gapFills,
		List<@NotNull @Valid SubmitZoneClickAnswer> zoneClicks,
		List<@NotNull @Valid SubmitPuzzlePiecePlacement> puzzlePlacements,
		@Size(max = 500) String attempt,
		@Size(max = 8) List<@NotNull @PositiveOrZero Integer> simonSequence) {

	Submission toPreviewSubmission() {
		if (formCode == null || challengeId != null) {
			throw QuestionPlayEngine.invalidAnswer();
		}
		return toSubmission();
	}

	Submission toSubmission() {
		List<Answer> variants = Stream.<Answer>of(
				selectedChoiceIds == null ? null : new ChildQuestionPlayUseCase.Choices(selectedChoiceIds),
				fromRows(statements, row -> new ChildQuestionPlayUseCase.StatementAnswer(row.statementId(), row.value()),
						ChildQuestionPlayUseCase.Statements::new),
				matchedPairIds == null ? null : new ChildQuestionPlayUseCase.LegacyMemory(matchedPairIds),
				fromRows(memoryMatches, SubmitMemoryMatch::match, ChildQuestionPlayUseCase.MemoryMatches::new),
				orderedItemIds == null ? null : new ChildQuestionPlayUseCase.Order(orderedItemIds),
				fromRows(placements, row -> new ChildQuestionPlayUseCase.PlacementAnswer(row.cardId(), row.columnId()),
						ChildQuestionPlayUseCase.Placements::new),
				fromRows(linkMatches, row -> new ChildQuestionPlayUseCase.LinkMatchAnswer(row.leftId(), row.rightId()),
						ChildQuestionPlayUseCase.LegacyLinks::new),
				fromRows(linkSelections, row -> new ChildQuestionPlayUseCase.LinkSelection(row.leftId(), row.rightId()),
						ChildQuestionPlayUseCase.LinkSelections::new),
				fromRows(gapFills, row -> new ChildQuestionPlayUseCase.GapFillAnswer(row.holeIndex(), row.wordId()),
						ChildQuestionPlayUseCase.GapFills::new),
				fromRows(zoneClicks, row -> new ChildQuestionPlayUseCase.ZoneClickAnswer(row.zoneId()),
						ChildQuestionPlayUseCase.ImageClicks::new),
				fromRows(puzzlePlacements, row -> new ChildQuestionPlayUseCase.PuzzlePiecePlacementAnswer(
						row.pieceId(), row.x(), row.y()), ChildQuestionPlayUseCase.Puzzle::new),
				attempt == null ? null : new ChildQuestionPlayUseCase.Text(attempt),
				simonSequence == null ? null : new ChildQuestionPlayUseCase.Simon(simonSequence))
				.filter(Objects::nonNull).toList();
		if (variants.size() != 1 || (formCode != null && (matchedPairIds != null || linkMatches != null))) {
			throw QuestionPlayEngine.invalidAnswer();
		}
		return new Submission(formCode, variants.getFirst(), submissionId, challengeId, playId);
	}

	private static <T, R> Answer fromRows(List<T> rows, Function<T, R> map, Function<List<R>, Answer> wrap) {
		return rows == null ? null : wrap.apply(rows.stream().map(map).toList());
	}

	public record SubmitMemoryMatch(@NotNull UUID firstCardId, @NotNull UUID secondCardId) {
		MemoryMatch match() {
			return new MemoryMatch(firstCardId, secondCardId);
		}
	}

	public record SubmitStatementAnswer(@NotNull @Positive Long statementId, @NotNull Boolean value) {
	}

	public record SubmitPlacementAnswer(@NotNull @Positive Long cardId, @NotNull @Positive Long columnId) {
	}

	public record SubmitLinkMatchAnswer(@NotNull @Positive Long leftId, @NotNull @Positive Long rightId) {
	}

	public record SubmitLinkSelection(@NotNull UUID leftId, @NotNull UUID rightId) {
	}

	public record SubmitGapFillAnswer(@NotNull @PositiveOrZero Integer holeIndex, @NotNull @Positive Long wordId) {
	}

	public record SubmitZoneClickAnswer(@NotNull @Positive Long zoneId) {
	}

	public record SubmitPuzzlePiecePlacement(
			@NotNull @Positive Long pieceId,
			@NotNull @DecimalMin("-0.5") @DecimalMax("1.5") Double x,
			@NotNull @DecimalMin("-0.5") @DecimalMax("1.5") Double y) {
	}
}
