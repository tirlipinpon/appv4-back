package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.SignupWithEmailUseCase;
import com.appv4.auth.application.result.JwtTokenResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Inscription email/mdp (SCRUM-105) — endpoint public.
 *
 * <p><b>Business :</b> créer un compte admin sans OAuth ; renvoyer un JWT
 * pour entrer directement dans l'app (comme après OAuth).
 * <p><b>Technique :</b> {@code POST /api/int/auth/signup} en permitAll.
 * <p><b>Métier / flow :</b> adapter in → {@link SignupWithEmailUseCase} →
 * BCrypt + persistance → {@code IssueJwtUseCase}.
 */
@RestController
@RequestMapping("/api/int/auth/signup")
public class SignupController {

	private final SignupWithEmailUseCase signupWithEmailUseCase;

	public SignupController(SignupWithEmailUseCase signupWithEmailUseCase) {
		this.signupWithEmailUseCase = signupWithEmailUseCase;
	}

	@PostMapping
	public ResponseEntity<JwtTokenResponse> signup(@Valid @RequestBody SignupRequest request) {
		JwtTokenResult result = signupWithEmailUseCase.signup(
				request.email(),
				request.password(),
				request.displayName());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(new JwtTokenResponse(result.token(), result.expiresAtEpochSeconds()));
	}

	public record SignupRequest(
			@NotBlank @Email String email,
			@NotBlank String password,
			String displayName) {
	}

	public record JwtTokenResponse(String token, long expiresAtEpochSeconds) {
	}
}
