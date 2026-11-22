package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.AskChildHumeurUseCase;
import com.appv4.auth.application.port.in.AskChildHumeurUseCase.Prompt;
import com.appv4.auth.application.port.out.HumeurChoicePort.HumeurChoice;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Demande d'humeur sur l'accueil enfant (BACK-69).
 *
 * <p><b>Business :</b> l'enfant choisit un choix renvoyé, ou ferme. Il ne voit pas la tendance.
 * <p><b>Technique :</b> {@code /api/ext/humeur} avec Bearer enfant. Liste vide si on ne demande pas.
 * <p><b>Métier / flow :</b> adapter in → {@link AskChildHumeurUseCase}.
 */
@RestController
@RequestMapping("/api/ext/humeur")
@Tag(name = "Enfant — humeur")
public class ChildHumeurController {

	private final AskChildHumeurUseCase askChildHumeurUseCase;

	public ChildHumeurController(AskChildHumeurUseCase askChildHumeurUseCase) {
		this.askChildHumeurUseCase = askChildHumeurUseCase;
	}

	@GetMapping
	@Operation(operationId = "askChildHumeur", summary = "Savoir s'il faut demander l'humeur, et les choix")
	@ApiResponse(responseCode = "200", description = "Demande, ou liste vide")
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildHumeurPromptResponse> ask(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		Prompt prompt = askChildHumeurUseCase.ask(bearer(authorization));
		List<ChildHumeurChoiceItem> choices = prompt.choices().stream()
				.map(ChildHumeurController::toItem)
				.toList();
		return ResponseEntity.ok(new ChildHumeurPromptResponse(prompt.ask(), choices));
	}

	@PostMapping
	@Operation(operationId = "answerChildHumeur", summary = "Enregistrer un choix ou une fermeture")
	@ApiResponse(responseCode = "204", description = "Réponse gardée")
	@ApiResponse(
			responseCode = "400",
			description = "Corps ni choix ni fermeture, ou choix inconnu",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<Void> answer(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestBody ChildHumeurAnswerRequest request) {
		askChildHumeurUseCase.answer(bearer(authorization), request.choiceId(), request.closed());
		return ResponseEntity.noContent().build();
	}

	private static ChildHumeurChoiceItem toItem(HumeurChoice choice) {
		return new ChildHumeurChoiceItem(choice.id(), choice.icon(), choice.word());
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}

	@Schema(name = "ChildHumeurAnswerRequest")
	public record ChildHumeurAnswerRequest(Long choiceId, Boolean closed) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(name = "ChildHumeurChoiceItem")
	public record ChildHumeurChoiceItem(long id, String icon, String word) {
	}

	@Schema(name = "ChildHumeurPromptResponse")
	public record ChildHumeurPromptResponse(boolean ask, List<ChildHumeurChoiceItem> choices) {
	}
}
