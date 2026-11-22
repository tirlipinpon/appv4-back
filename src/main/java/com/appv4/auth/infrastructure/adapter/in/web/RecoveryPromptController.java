package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.RecoveryPromptUseCase;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Réponse à la sollicitation téléphone / mail de contact.
 *
 * <p><b>Business :</b> passer n'écrit pas ces infos. Enregistrer les garde et ne redemande pas.
 * Un jeton enfant n'ouvre pas cette porte : le filtre adulte le refuse déjà.
 * <p><b>Technique :</b> POST {@code /api/int/auth/recovery-prompt}, JWT adulte.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → cas d'usage.
 */
@RestController
@RequestMapping("/api/int/auth/recovery-prompt")
@Tag(name = "Auth — sollicitation de contact")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class RecoveryPromptController {

	private final RecoveryPromptUseCase recoveryPromptUseCase;

	public RecoveryPromptController(RecoveryPromptUseCase recoveryPromptUseCase) {
		this.recoveryPromptUseCase = recoveryPromptUseCase;
	}

	@PostMapping
	@Operation(operationId = "answerRecoveryPrompt", summary = "Passer ou enregistrer la sollicitation de contact")
	public ResponseEntity<Void> answer(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestBody RecoveryPromptRequest request) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		boolean skip = Boolean.TRUE.equals(request.skip());
		recoveryPromptUseCase.answer(user.userId(), skip, request.displayName(), request.phone(), request.contactEmail());
		return ResponseEntity.noContent().build();
	}

	@Schema(name = "RecoveryPromptRequest")
	public record RecoveryPromptRequest(Boolean skip, String displayName, String phone, String contactEmail) {
	}
}
