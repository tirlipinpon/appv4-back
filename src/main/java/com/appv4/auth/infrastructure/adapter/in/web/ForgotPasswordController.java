package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ForgotPasswordUseCase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Forgot-password public (SCRUM-108).
 *
 * <p><b>Business :</b> l'utilisateur demande un lien sans révéler si l'email existe.
 * <p><b>Technique :</b> {@code POST /api/int/auth/forgot-password} en permitAll.
 */
@RestController
@RequestMapping("/api/int/auth/forgot-password")
public class ForgotPasswordController {

	private final ForgotPasswordUseCase forgotPasswordUseCase;

	public ForgotPasswordController(ForgotPasswordUseCase forgotPasswordUseCase) {
		this.forgotPasswordUseCase = forgotPasswordUseCase;
	}

	@PostMapping
	public ResponseEntity<MessageResponse> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
		String message = forgotPasswordUseCase.requestReset(request.email());
		return ResponseEntity.ok(new MessageResponse(message));
	}

	public record ForgotPasswordRequest(@NotBlank @Email String email) {
	}

	public record MessageResponse(String message) {
	}
}
