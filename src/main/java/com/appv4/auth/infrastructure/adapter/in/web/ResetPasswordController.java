package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ResetPasswordUseCase;
import com.appv4.auth.application.service.ResetPasswordService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Reset-password public (SCRUM-111).
 *
 * <p><b>Business :</b> l'utilisateur change son mdp via le lien email (token + newPassword).
 * <p><b>Technique :</b> {@code POST /api/int/auth/reset-password} en permitAll.
 * <p><b>Métier / flow :</b> adapter in → {@link ResetPasswordUseCase} → ports out token + user.
 */
@RestController
@RequestMapping("/api/int/auth/reset-password")
public class ResetPasswordController {

	private final ResetPasswordUseCase resetPasswordUseCase;

	public ResetPasswordController(ResetPasswordUseCase resetPasswordUseCase) {
		this.resetPasswordUseCase = resetPasswordUseCase;
	}

	@PostMapping
	public ResponseEntity<MessageResponse> reset(@Valid @RequestBody ResetPasswordRequest request) {
		resetPasswordUseCase.reset(request.token(), request.newPassword());
		return ResponseEntity.ok(new MessageResponse(ResetPasswordService.SUCCESS_MESSAGE));
	}

	public record ResetPasswordRequest(
			@NotBlank String token,
			@NotBlank String newPassword) {
	}

	public record MessageResponse(String message) {
	}
}
