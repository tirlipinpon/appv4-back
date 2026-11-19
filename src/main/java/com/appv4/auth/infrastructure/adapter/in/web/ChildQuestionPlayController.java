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
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.GapFillAnswer;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.LinkMatchAnswer;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.PlacementAnswer;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.PuzzlePiecePlacementAnswer;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.StatementAnswer;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.ZoneClickAnswer;
import com.appv4.auth.application.result.ChildAttemptResultView;
import com.appv4.auth.application.result.ChildLetterLocksView;
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
		List<LinkMatchAnswer> linkMatches = body == null || body.linkMatches() == null
				? List.of()
				: body.linkMatches().stream()
						.map(m -> new LinkMatchAnswer(m.leftId(), m.rightId()))
						.toList();
		List<GapFillAnswer> gapFills = body == null || body.gapFills() == null
				? List.of()
				: body.gapFills().stream()
						.map(fill -> new GapFillAnswer(fill.holeIndex(), fill.wordId()))
						.toList();
		List<ZoneClickAnswer> zoneClicks = body == null || body.zoneClicks() == null
				? List.of()
				: body.zoneClicks().stream()
						.map(click -> new ZoneClickAnswer(click.zoneId()))
						.toList();
		List<PuzzlePiecePlacementAnswer> puzzlePlacements = body == null || body.puzzlePlacements() == null
				? List.of()
				: body.puzzlePlacements().stream()
						.map(p -> new PuzzlePiecePlacementAnswer(p.pieceId(), p.x(), p.y()))
						.toList();
		AttemptAnswers answers = new AttemptAnswers(
				body == null || body.selectedChoiceIds() == null ? List.of() : body.selectedChoiceIds(),
				statements,
				body == null || body.matchedPairIds() == null ? List.of() : body.matchedPairIds(),
				body == null || body.orderedItemIds() == null ? List.of() : body.orderedItemIds(),
				placements,
				linkMatches,
				gapFills,
				zoneClicks,
				puzzlePlacements,
				body == null ? null : body.attempt(),
				body == null ? null : body.simonSequence());
		ChildAttemptResultView view =
				childQuestionPlayUseCase.submit(bearer(authorization), subjectId, questionId, answers);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new ChildAttemptResultResponse(view.attemptId(), view.scorePercent(), view.passed()));
	}

	@PostMapping("/letter-locks")
	@Operation(
			operationId = "checkChildLetterLocks",
			summary = "Verrouiller les lettres déjà justes (sans enregistrer)")
	@ApiResponse(responseCode = "200", description = "Verrous par case")
	@ApiResponse(
			responseCode = "400",
			description = "Forme non lettre par lettre",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière ou question absente / non liée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildLetterLocksResponse> letterLocks(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable long subjectId,
			@PathVariable long questionId,
			@RequestBody ChildLetterLocksRequest body) {
		ChildLetterLocksView view = childQuestionPlayUseCase.letterLocks(
				bearer(authorization),
				subjectId,
				questionId,
				body == null ? "" : body.attempt());
		return ResponseEntity.ok(new ChildLetterLocksResponse(view.locks()));
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
		List<ChildPlayLinkItem> links = view.links().stream()
				.map(link -> new ChildPlayLinkItem(link.linkId(), link.leftLabel(), link.rightLabel()))
				.toList();
		List<ChildPlayGapWordItem> words = view.words().stream()
				.map(word -> new ChildPlayGapWordItem(word.wordId(), word.label()))
				.toList();
		List<ChildPlayClickZoneItem> clickZones = view.clickZones().stream()
				.map(zone -> new ChildPlayClickZoneItem(
						zone.zoneId(),
						zone.shape(),
						zone.points().stream()
								.map(point -> new ChildPlayClickPointItem(point.x(), point.y()))
								.toList()))
				.toList();
		List<ChildPlayClickStepItem> clickSteps = view.clickSteps().stream()
				.map(step -> new ChildPlayClickStepItem(step.zoneId(), step.prompt()))
				.toList();
		List<ChildPlayPuzzlePieceItem> puzzlePieces = view.puzzlePieces().stream()
				.map(piece -> new ChildPlayPuzzlePieceItem(
						piece.pieceId(),
						piece.label(),
						piece.shape(),
						piece.points().stream()
								.map(point -> new ChildPlayClickPointItem(point.x(), point.y()))
								.toList(),
						piece.slotX(),
						piece.slotY(),
						piece.slotWidth()))
				.toList();
		List<ChildPlaySimonElementItem> elements = view.elements().stream()
				.map(element -> new ChildPlaySimonElementItem(element.label(), element.color()))
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
				view.timelineEnabled(),
				links,
				view.sentence(),
				words,
				view.imageWebp(),
				view.clicksOrdered(),
				clickZones,
				clickSteps,
				view.letterCount(),
				view.letterMask(),
				view.boardMode(),
				view.boardImageWebp(),
				view.snapDistance(),
				puzzlePieces,
				view.elementType(),
				elements,
				view.sequence());
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
			Boolean timelineEnabled,
			List<ChildPlayLinkItem> links,
			String sentence,
			List<ChildPlayGapWordItem> words,
			String imageWebp,
			Boolean clicksOrdered,
			List<ChildPlayClickZoneItem> clickZones,
			List<ChildPlayClickStepItem> clickSteps,
			Integer letterCount,
			String letterMask,
			String boardMode,
			String boardImageWebp,
			Double snapDistance,
			List<ChildPlayPuzzlePieceItem> puzzlePieces,
			@Schema(description = "Simon seulement : couleurs, nombres, lettres ou formes.")
			String elementType,
			@Schema(description = "Simon seulement : éléments à afficher, non mélangés. "
					+ "Leur position (0, 1, 2…) est celle que sequence désigne et que simonSequence renvoie.")
			List<ChildPlaySimonElementItem> elements,
			@Schema(description = "Simon seulement : la suite à montrer, dans l'ordre où les éléments s'allument "
					+ "(positions 0-based dans elements).")
			List<Integer> sequence) {
	}

	@Schema(name = "ChildPlaySimonElementItem")
	public record ChildPlaySimonElementItem(
			String label,
			@Schema(description = "Code #rrggbb pour le genre couleurs. Absent pour les autres genres.")
			String color) {
	}

	@Schema(name = "ChildLetterLocksRequest")
	public record ChildLetterLocksRequest(String attempt) {
	}

	@Schema(name = "ChildLetterLocksResponse")
	public record ChildLetterLocksResponse(List<Boolean> locks) {
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

	@Schema(name = "ChildPlayLinkItem")
	public record ChildPlayLinkItem(long linkId, String leftLabel, String rightLabel) {
	}

	@Schema(name = "ChildPlayGapWordItem")
	public record ChildPlayGapWordItem(long wordId, String label) {
	}

	@Schema(name = "ChildPlayClickZoneItem")
	public record ChildPlayClickZoneItem(long zoneId, String shape, List<ChildPlayClickPointItem> points) {
	}

	@Schema(name = "ChildPlayClickPointItem")
	public record ChildPlayClickPointItem(double x, double y) {
	}

	@Schema(name = "ChildPlayClickStepItem")
	public record ChildPlayClickStepItem(long zoneId, String prompt) {
	}

	@Schema(name = "ChildPlayPuzzlePieceItem")
	public record ChildPlayPuzzlePieceItem(
			long pieceId,
			String label,
			String shape,
			List<ChildPlayClickPointItem> points,
			Double slotX,
			Double slotY,
			@Schema(description = "Largeur de la pièce sur le fond dédié (0–1 de la largeur du plateau). "
					+ "Absente : taille de la découpe.")
			Double slotWidth) {
	}

	@Schema(name = "SubmitChildAttemptRequest")
	public record SubmitChildAttemptRequest(
			List<Long> selectedChoiceIds,
			List<SubmitStatementAnswer> statements,
			List<Long> matchedPairIds,
			List<Long> orderedItemIds,
			List<SubmitPlacementAnswer> placements,
			List<SubmitLinkMatchAnswer> linkMatches,
			List<SubmitGapFillAnswer> gapFills,
			List<SubmitZoneClickAnswer> zoneClicks,
			List<SubmitPuzzlePiecePlacement> puzzlePlacements,
			String attempt,
			@Schema(description = "Simon seulement : positions 0-based des éléments touchés, dans l'ordre, "
					+ "une par pas de la suite (même longueur).")
			List<Integer> simonSequence) {
	}

	@Schema(name = "SubmitStatementAnswer")
	public record SubmitStatementAnswer(long statementId, boolean value) {
	}

	@Schema(name = "SubmitPlacementAnswer")
	public record SubmitPlacementAnswer(long cardId, long columnId) {
	}

	@Schema(name = "SubmitLinkMatchAnswer")
	public record SubmitLinkMatchAnswer(long leftId, long rightId) {
	}

	@Schema(name = "SubmitGapFillAnswer")
	public record SubmitGapFillAnswer(int holeIndex, long wordId) {
	}

	@Schema(name = "SubmitZoneClickAnswer")
	public record SubmitZoneClickAnswer(long zoneId) {
	}

	@Schema(name = "SubmitPuzzlePiecePlacement")
	public record SubmitPuzzlePiecePlacement(long pieceId, double x, double y) {
	}

	@Schema(name = "ChildAttemptResultResponse")
	public record ChildAttemptResultResponse(long attemptId, int scorePercent, boolean passed) {
	}
}
