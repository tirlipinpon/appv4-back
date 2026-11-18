package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.AttemptAnswers;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.PlacementAnswer;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.StatementAnswer;
import com.appv4.auth.application.result.ChildAttemptResultView;
import com.appv4.auth.application.result.ChildPlayQuestionView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Jouer une question enfant : détail sans solution + tentative (BACK-16).
 *
 * <p><b>Business :</b> Learn affiche le score renvoyé, sans le recalculer.
 * <p><b>Technique :</b> {@code GET/POST /api/ext/subjects/{id}/questions/{qid}...}
 * <p><b>Métier / flow :</b> adapter in → {@link ChildQuestionPlayUseCase}.
 */
@RestController
@RequestMapping("/api/ext/subjects/{subjectId}/questions/{questionId}")
@Tag(name = "Enfant — jeu")
public class ChildQuestionPlayController {

	private final ChildQuestionPlayUseCase childQuestionPlayUseCase;

	public ChildQuestionPlayController(ChildQuestionPlayUseCase childQuestionPlayUseCase) {
		this.childQuestionPlayUseCase = childQuestionPlayUseCase;
	}

	@GetMapping
	@Operation(operationId = "getChildPlayQuestion", summary = "Lire une question jouable (sans solution)")
	@ApiResponse(responseCode = "200", description = "Contenu jouable")
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière ou question absente / non liée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildPlayQuestionResponse> get(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable long subjectId,
			@PathVariable long questionId) {
		ChildPlayQuestionView view = childQuestionPlayUseCase.question(bearer(authorization), subjectId, questionId);
		return ResponseEntity.ok(toResponse(view));
	}

	@PostMapping("/attempts")
	@Operation(operationId = "submitChildQuestionAttempt", summary = "Enregistrer une tentative et renvoyer le score")
	@ApiResponse(responseCode = "201", description = "Tentative enregistrée")
	@ApiResponse(
			responseCode = "400",
			description = "Réponse invalide ou forme non jouable",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière ou question absente / non liée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildAttemptResultResponse> submit(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable long subjectId,
			@PathVariable long questionId,
			@RequestBody SubmitChildAttemptRequest body) {
		List<StatementAnswer> statements = body == null || body.statements() == null
				? List.of()
				: body.statements().stream()
						.map(s -> new StatementAnswer(s.statementId(), s.value()))
						.toList();
		List<PlacementAnswer> placements = body == null || body.placements() == null
				? List.of()
				: body.placements().stream()
						.map(p -> new PlacementAnswer(p.cardId(), p.columnId()))
						.toList();
		AttemptAnswers answers = new AttemptAnswers(
				body == null || body.selectedChoiceIds() == null ? List.of() : body.selectedChoiceIds(),
				statements,
				body == null || body.matchedPairIds() == null ? List.of() : body.matchedPairIds(),
				body == null || body.orderedItemIds() == null ? List.of() : body.orderedItemIds(),
				placements);
		ChildAttemptResultView view =
				childQuestionPlayUseCase.submit(bearer(authorization), subjectId, questionId, answers);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new ChildAttemptResultResponse(view.attemptId(), view.scorePercent(), view.passed()));
	}

	private static ChildPlayQuestionResponse toResponse(ChildPlayQuestionView view) {
		List<ChildPlayChoiceItem> choices = view.choices().stream()
				.map(c -> new ChildPlayChoiceItem(c.choiceId(), c.label()))
				.toList();
		List<ChildPlayStatementItem> statements = view.statements().stream()
				.map(s -> new ChildPlayStatementItem(s.statementId(), s.label()))
				.toList();
		List<ChildPlayPairItem> pairs = view.pairs().stream()
				.map(p -> new ChildPlayPairItem(p.pairId(), p.questionSide(), p.answerSide()))
				.toList();
		List<ChildPlayItemItem> items = view.items().stream()
				.map(i -> new ChildPlayItemItem(i.itemId(), i.label(), i.axisLabel()))
				.toList();
		List<ChildPlayColumnItem> columns = view.columns().stream()
				.map(c -> new ChildPlayColumnItem(c.columnId(), c.label()))
				.toList();
		List<ChildPlayCardItem> cards = view.cards().stream()
				.map(c -> new ChildPlayCardItem(c.cardId(), c.label()))
				.toList();
		return new ChildPlayQuestionResponse(
				view.questionId(),
				view.subjectId(),
				view.formCode(),
				view.prompt(),
				choices,
				statements,
				pairs,
				items,
				columns,
				cards,
				view.timelineEnabled());
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}

	@Schema(name = "ChildPlayQuestionResponse")
	public record ChildPlayQuestionResponse(
			long questionId,
			long subjectId,
			String formCode,
			String prompt,
			List<ChildPlayChoiceItem> choices,
			List<ChildPlayStatementItem> statements,
			List<ChildPlayPairItem> pairs,
			List<ChildPlayItemItem> items,
			List<ChildPlayColumnItem> columns,
			List<ChildPlayCardItem> cards,
			Boolean timelineEnabled) {
	}

	@Schema(name = "ChildPlayChoiceItem")
	public record ChildPlayChoiceItem(long choiceId, String label) {
	}

	@Schema(name = "ChildPlayStatementItem")
	public record ChildPlayStatementItem(long statementId, String label) {
	}

	@Schema(name = "ChildPlayPairItem")
	public record ChildPlayPairItem(long pairId, String questionSide, String answerSide) {
	}

	@Schema(name = "ChildPlayItemItem")
	public record ChildPlayItemItem(long itemId, String label, String axisLabel) {
	}

	@Schema(name = "ChildPlayColumnItem")
	public record ChildPlayColumnItem(long columnId, String label) {
	}

	@Schema(name = "ChildPlayCardItem")
	public record ChildPlayCardItem(long cardId, String label) {
	}

	@Schema(name = "SubmitChildAttemptRequest")
	public record SubmitChildAttemptRequest(
			List<Long> selectedChoiceIds,
			List<SubmitStatementAnswer> statements,
			List<Long> matchedPairIds,
			List<Long> orderedItemIds,
			List<SubmitPlacementAnswer> placements) {
	}

	@Schema(name = "SubmitStatementAnswer")
	public record SubmitStatementAnswer(long statementId, boolean value) {
	}

	@Schema(name = "SubmitPlacementAnswer")
	public record SubmitPlacementAnswer(long cardId, long columnId) {
	}

	@Schema(name = "ChildAttemptResultResponse")
	public record ChildAttemptResultResponse(long attemptId, int scorePercent, boolean passed) {
	}
}
