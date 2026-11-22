package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.RecoverIdentifierUseCase;
import com.appv4.auth.application.port.in.RecoverIdentifierUseCase.IdentifierRecovery;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Récupération de l'identifiant, route publique.
 *
 * <p><b>Business :</b> la personne saisit le nom affiché, le téléphone et le mail de contact.
 * Elle reçoit un masque, ou une phrase. Jamais l'email de connexion entier.
 * <p><b>Technique :</b> {@code POST /api/int/auth/recover-identifier} en permitAll.
 * <p><b>Métier / flow :</b> adaptateur web → cas d'usage → lecture des comptes.
 */
@RestController
@RequestMapping("/api/int/auth/recover-identifier")
@Tag(name = "Auth — récupération de l'identifiant")
public class RecoverIdentifierController {

	private final RecoverIdentifierUseCase recoverIdentifierUseCase;

	public RecoverIdentifierController(RecoverIdentifierUseCase recoverIdentifierUseCase) {
		this.recoverIdentifierUseCase = recoverIdentifierUseCase;
	}

	@PostMapping
	@Operation(operationId = "recoverIdentifier", summary = "Retrouver l'email de connexion masqué")
	public ResponseEntity<RecoverIdentifierResponse> recover(@RequestBody RecoverIdentifierRequest request) {
		IdentifierRecovery result = recoverIdentifierUseCase.recover(
				request.displayName(),
				request.phone(),
				request.contactEmail());
		return ResponseEntity.ok(new RecoverIdentifierResponse(result.maskedEmail(), result.message()));
	}

	@Schema(name = "RecoverIdentifierRequest")
	public record RecoverIdentifierRequest(String displayName, String phone, String contactEmail) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(name = "RecoverIdentifierResponse")
	public record RecoverIdentifierResponse(String maskedEmail, String message) {
	}
}
