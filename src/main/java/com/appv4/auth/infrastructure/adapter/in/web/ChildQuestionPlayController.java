package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;
import java.util.UUID;

import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.PlayAllowance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.CacheControl;
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
import com.appv4.auth.application.result.ChildAttemptResultView;
import com.appv4.auth.application.result.ChildPlayQuestionView.Payload;
import com.appv4.auth.application.service.question.QuestionFormRegistry;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Parcours enfant. Le score et l'état d'une partie sont calculés côté serveur. */
@RestController
@RequestMapping("/api/ext/subjects/{subjectId}/questions/{questionId}")
@Tag(name = "Enfant — jeu")
public class ChildQuestionPlayController {
	private final ChildQuestionPlayUseCase play;
	private final QuestionFormRegistry forms;

	public ChildQuestionPlayController(ChildQuestionPlayUseCase play, QuestionFormRegistry forms) {
		this.play = play;
		this.forms = forms;
	}

	@GetMapping
	@Operation(operationId = "getChildPlayQuestion", summary = "Lire une question pour un ancien client", deprecated = true)
	@ApiResponse(responseCode = "200", description = "Contenu jouable avec les champs historiques à plat")
	@ApiResponse(responseCode = "401", description = "Badge enfant invalide",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(responseCode = "404", description = "Matière ou question absente",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildPlayQuestionResponse> get(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable long subjectId, @PathVariable long questionId) {
		String token = bearer(authorization);
		PlayAllowance allowance = play.allowance(token, subjectId, questionId, null);
		PlayQuestionResponse body = PlayQuestionResponse.from(
				play.question(token, subjectId, questionId, true), forms,
				allowance.maxAttempts(), allowance.attemptsRemaining());
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ChildPlayQuestionResponse(
				body.questionId(), body.subjectId(), body.formCode(), body.prompt(), body.formLabel(), body.iconCode(),
				body.color(), body.challengeId(), body.content()));
	}

	@PostMapping("/play")
	@Operation(operationId = "openChildPlayQuestion", summary = "Ouvrir une partie sur une question")
	@ApiResponse(responseCode = "200", description = "Contenu jouable correspondant à la forme")
	@ApiResponse(responseCode = "401", description = "Badge enfant invalide",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(responseCode = "404", description = "Matière ou question absente",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<PlayQuestionResponse> open(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable long subjectId, @PathVariable long questionId,
			@RequestBody(required = false) OpenPlayRequest body) {
		String token = bearer(authorization);
		UUID playId = body == null ? null : body.playId();
		PlayAllowance allowance = play.allowance(token, subjectId, questionId, playId);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(PlayQuestionResponse.from(
				play.question(token, subjectId, questionId, false), forms,
				allowance.maxAttempts(), allowance.attemptsRemaining()));
	}

	@PostMapping("/attempts")
	@Operation(operationId = "submitChildQuestionAttempt", summary = "Enregistrer une tentative et renvoyer le score")
	@ApiResponse(responseCode = "201", description = "Verdict de la soumission, y compris en cas de relance du même identifiant")
	@ApiResponse(responseCode = "400", description = "Réponse invalide ou incompatible avec la forme",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(responseCode = "401", description = "Badge enfant invalide",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(responseCode = "404", description = "Matière ou question absente",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(responseCode = "409",
			description = "Partie Simon expirée, clé de soumission utilisée ailleurs, ou page sans essai restant",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildAttemptResultResponse> submit(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable long subjectId, @PathVariable long questionId,
			@Valid @RequestBody PlayAttemptRequest body) {
		ChildAttemptResultView result = play.submit(bearer(authorization), subjectId, questionId, body.toSubmission());
		return ResponseEntity.status(HttpStatus.CREATED).body(new ChildAttemptResultResponse(
				result.attemptId(), result.scorePercent(), result.passed(), result.attemptRank(),
				result.attemptsRemaining(), result.pointsAwarded(), result.replayAvailable()));
	}

	@PostMapping("/letter-locks")
	@Operation(operationId = "checkChildLetterLocks", summary = "Vérifier les lettres déjà saisies")
	public ResponseEntity<ChildLetterLocksResponse> letterLocks(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable long subjectId, @PathVariable long questionId,
			@Valid @RequestBody ChildLetterLocksRequest body) {
		return ResponseEntity.ok(new ChildLetterLocksResponse(
				play.letterLocks(bearer(authorization), subjectId, questionId, body.attempt()).locks()));
	}

	@PostMapping("/memory-pair")
	@Operation(operationId = "checkChildMemoryPair", summary = "Vérifier deux cartes retournées")
	public ResponseEntity<MemoryPairResponse> memoryPair(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable long subjectId, @PathVariable long questionId,
			@Valid @RequestBody PlayAttemptRequest.SubmitMemoryMatch request) {
		return ResponseEntity.ok(new MemoryPairResponse(
				play.memoryPair(bearer(authorization), subjectId, questionId, request.match())));
	}

	private static String bearer(String authorization) {
		return authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7).trim() : "";
	}

	public record ChildPlayQuestionResponse(
			long questionId, long subjectId, String formCode, String prompt, String formLabel, String iconCode,
			String color, UUID challengeId, @JsonUnwrapped Payload content) {
	}

	public record ChildLetterLocksRequest(@NotNull @Size(max = 500) String attempt) {
	}

	public record ChildLetterLocksResponse(List<Boolean> locks) {
	}

	public record MemoryPairResponse(boolean matched) {
	}

	public record OpenPlayRequest(UUID playId) {
	}

	public record ChildAttemptResultResponse(
			long attemptId, int scorePercent, boolean passed, int attemptRank, int attemptsRemaining,
			int pointsAwarded, boolean replayAvailable) {
	}
}
