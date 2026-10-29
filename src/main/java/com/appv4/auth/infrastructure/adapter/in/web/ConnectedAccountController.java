package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ConnectedAccountUseCase;
import com.appv4.auth.application.port.in.ConnectedAccountUseCase.ConnectedBanner;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Nom de la personne connectée, pour le bandeau (parent ou professeur).
 *
 * <p><b>Business :</b> l'en-tête montre qui est connecté, y compris après F5
 * et après un changement de nom sur le profil.
 * <p><b>Technique :</b> GET {@code /api/int/auth/me}, JWT obligatoire.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link ConnectedAccountUseCase}.
 */
@RestController
@RequestMapping("/api/int/auth/me")
@Tag(name = "Auth — compte connecté")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class ConnectedAccountController {

	private final ConnectedAccountUseCase connectedAccountUseCase;

	public ConnectedAccountController(ConnectedAccountUseCase connectedAccountUseCase) {
		this.connectedAccountUseCase = connectedAccountUseCase;
	}

	@GetMapping
	@Operation(operationId = "getConnectedAccount", summary = "Nom de la personne connectée")
	@ApiResponse(responseCode = "200", description = "Nom à afficher dans l'en-tête")
	@ApiResponse(
			responseCode = "404",
			description = "Compte introuvable",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ConnectedAccountResponse> view(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		ConnectedBanner banner = connectedAccountUseCase.banner(user.userId());
		return ResponseEntity.ok(new ConnectedAccountResponse(banner.displayName(), banner.avatarCode()));
	}
}
