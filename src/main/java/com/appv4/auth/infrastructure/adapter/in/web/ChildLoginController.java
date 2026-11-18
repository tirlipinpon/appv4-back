package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.LoginChildUseCase;
import com.appv4.auth.application.result.ChildLoginResult;
import com.appv4.auth.application.result.ChildSessionView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Entrée de l'app enfant (BACK-13).
 *
 * <p><b>Business :</b> l'enfant envoie son prénom et son code. Le badge reçu
 * ne sert pas à appeler l'admin.
 * <p><b>Technique :</b> {@code POST /api/ext/auth/login} public, limité en débit.
 * {@code GET /api/ext/auth/session} relit la fiche si le badge enfant est encore valable.
 * <p><b>Métier / flow :</b> adapter in → {@link LoginChildUseCase}.
 */
@RestController
@RequestMapping("/api/ext/auth")
@Tag(name = "Enfant — session")
public class ChildLoginController {

	private final LoginChildUseCase loginChildUseCase;

	public ChildLoginController(LoginChildUseCase loginChildUseCase) {
		this.loginChildUseCase = loginChildUseCase;
	}

	@PostMapping("/login")
	@Operation(operationId = "loginChild", summary = "Connexion enfant par prénom et code")
	@ApiResponse(responseCode = "200", description = "Session enfant ouverte")
	@ApiResponse(
			responseCode = "401",
			description = "Prénom ou code refusé (message unique)",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildLoginResponse> login(@RequestBody ChildLoginRequest request) {
		ChildLoginResult result = loginChildUseCase.login(request.firstName(), request.pin());
		return ResponseEntity.ok(new ChildLoginResponse(
				result.token(), result.expiresAtEpochSeconds(), result.firstName()));
	}

	@GetMapping("/session")
	@Operation(operationId = "readChildSession", summary = "Relire l'enfant de la session")
	@ApiResponse(responseCode = "200", description = "Fiche encore jouable")
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildSessionResponse> session(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		ChildSessionView view = loginChildUseCase.session(bearer(authorization));
		return ResponseEntity.ok(new ChildSessionResponse(view.childId(), view.firstName()));
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}

	@Schema(name = "ChildLoginRequest")
	public record ChildLoginRequest(String firstName, String pin) {
	}

	@Schema(name = "ChildLoginResponse")
	public record ChildLoginResponse(String token, long expiresAtEpochSeconds, String firstName) {
	}

	@Schema(name = "ChildSessionResponse")
	public record ChildSessionResponse(long childId, String firstName) {
	}
}
