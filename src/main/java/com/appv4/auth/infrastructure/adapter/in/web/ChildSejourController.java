package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.SejourUseCase;
import com.appv4.auth.application.port.in.SejourUseCase.ChildSejour;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Signaux et lecture du séjour pour l'enfant (BACK-38, question BACK-79, popup BACK-81).
 *
 * <p><b>Business :</b> l'app envoie un signal, y compris « Je continue ».
 * Un jeu porte l'identifiant de la question. Elle lit ensuite la popup,
 * et si cet appareil tient encore le séjour. Elle ne compte pas.
 * <p><b>Technique :</b> {@code POST /api/ext/sejour/signals} et {@code GET /api/ext/sejour}
 * avec Bearer enfant. L'appareil est un identifiant opaque.
 * <p><b>Métier / flow :</b> adapter in → {@link SejourUseCase}.
 */
@RestController
@RequestMapping("/api/ext/sejour")
@Tag(name = "Enfant — séjour")
public class ChildSejourController {

	private final SejourUseCase sejourUseCase;

	public ChildSejourController(SejourUseCase sejourUseCase) {
		this.sejourUseCase = sejourUseCase;
	}

	@PostMapping("/signals")
	@Operation(operationId = "signalChildSejour", summary = "Envoyer un signal de séjour")
	@ApiResponse(responseCode = "204", description = "Signal accepté, ou sans effet s'il ne confirme rien")
	@ApiResponse(
			responseCode = "400",
			description = "Appareil absent, écran inconnu, ou signal refusé",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<Void> signal(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestBody ChildSejourSignalRequest request) {
		sejourUseCase.signal(
				bearer(authorization), request.kind(), request.deviceId(), request.screen(), request.questionId());
		return ResponseEntity.noContent().build();
	}

	@GetMapping
	@Operation(operationId = "readChildSejour", summary = "Lire la popup et si cet appareil tient le séjour")
	@ApiResponse(responseCode = "200", description = "Popup due, ou seulement qui tient le séjour")
	@ApiResponse(
			responseCode = "400",
			description = "Appareil absent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildSejourResponse> read(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestParam(name = "deviceId", required = false) String deviceId) {
		ChildSejour view = sejourUseCase.readChild(bearer(authorization), deviceId);
		return ResponseEntity.ok(new ChildSejourResponse(view.holds(), view.message(), view.button()));
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}

	/**
	 * @param questionId question jouée, exigée quand {@code kind} vaut {@code question}
	 */
	@Schema(name = "ChildSejourSignalRequest")
	public record ChildSejourSignalRequest(String kind, String deviceId, String screen, Long questionId) {
	}

	/**
	 * Réponse de la lecture enfant.
	 *
	 * <p><b>Business :</b> l'app affiche la phrase et le bouton tels quels, seulement
	 * quand la popup est due.
	 * <p><b>Technique :</b> {@code message} et {@code button} sont absents hors popup.
	 * <p><b>Métier / flow :</b> corps de {@code GET /api/ext/sejour}.
	 *
	 * @param message phrase de la popup, absente sinon
	 * @param button libellé du bouton, absent sinon
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(name = "ChildSejourResponse")
	public record ChildSejourResponse(boolean holds, String message, String button) {
	}
}
