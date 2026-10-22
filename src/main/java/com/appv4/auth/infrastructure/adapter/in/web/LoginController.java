package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.LoginWithEmailUseCase;
import com.appv4.auth.application.result.JwtTokenResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login email/mdp public (SCRUM-117).
 *
 * <p><b>Business :</b> se connecter à l'admin avec email + mdp (message unique si échec).
 * <p><b>Technique :</b> {@code POST /api/int/auth/login} en permitAll.
 * <p><b>Métier / flow :</b> adapter in → {@link LoginWithEmailUseCase} → JWT.
 */
@RestController
@RequestMapping("/api/int/auth/login")
@Tag(name = "Auth — login email")
public class LoginController {

	private final LoginWithEmailUseCase loginWithEmailUseCase;

	public LoginController(LoginWithEmailUseCase loginWithEmailUseCase) {
		this.loginWithEmailUseCase = loginWithEmailUseCase;
	}

	@PostMapping
	@Operation(summary = "Connexion email / mot de passe")
	@ApiResponse(responseCode = "200", description = "JWT émis")
	@ApiResponse(
			responseCode = "401",
			description = "Email ou mdp invalide (message unique anti-énumération)",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<JwtTokenResponse> login(@Valid @RequestBody LoginRequest request) {
		JwtTokenResult result = loginWithEmailUseCase.login(request.email(), request.password());
		return ResponseEntity.ok(new JwtTokenResponse(result.token(), result.expiresAtEpochSeconds()));
	}

	@Schema(name = "LoginRequest")
	public record LoginRequest(
			@NotBlank @Email String email,
			@NotBlank String password) {
	}

	@Schema(name = "JwtTokenResponse")
	public record JwtTokenResponse(String token, long expiresAtEpochSeconds) {
	}
}
