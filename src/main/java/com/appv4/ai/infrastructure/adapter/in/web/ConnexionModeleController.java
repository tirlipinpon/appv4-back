package com.appv4.ai.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.ai.application.ConnexionModele;
import com.appv4.ai.application.EssaiConnexion;
import com.appv4.ai.application.port.in.ConnexionModeleUseCase;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.infrastructure.adapter.in.web.CurrentAuth;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Connexions de modèles pour l'admin (BACK-84).
 *
 * <p><b>Business :</b> ajouter un modèle et l'essayer. La clé ne revient pas.
 * <p><b>Technique :</b> {@code /api/int/admin/ai/connections}. Le corps d'essai n'a pas de prompt.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link ConnexionModeleUseCase}.
 */
@RestController
@RequestMapping("/api/int/admin/ai/connections")
@Tag(name = "Admin — IA")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class ConnexionModeleController {

	private final ConnexionModeleUseCase connexions;

	public ConnexionModeleController(ConnexionModeleUseCase connexions) {
		this.connexions = connexions;
	}

	@GetMapping
	@Operation(operationId = "listAiConnections", summary = "Lire les connexions de modèles")
	public ResponseEntity<ConnexionListResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
		List<ConnexionResponse> items = connexions.lister(CurrentAuth.require(principal).userId()).stream()
				.map(ConnexionModeleController::toResponse)
				.toList();
		return ResponseEntity.ok(new ConnexionListResponse(items));
	}

	@PostMapping
	@Operation(operationId = "createAiConnection", summary = "Ajouter une connexion de modèle")
	public ResponseEntity<ConnexionResponse> create(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestBody CreateConnexionRequest request) {
		ConnexionModele saved = connexions.creer(
				CurrentAuth.require(principal).userId(),
				request.provider(),
				request.model(),
				request.apiKey());
		return ResponseEntity.ok(toResponse(saved));
	}

	@PostMapping("/{id}/test")
	@Operation(operationId = "testAiConnection", summary = "Essayer une connexion de modèle")
	public ResponseEntity<EssaiResponse> test(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id) {
		EssaiConnexion essai = connexions.essayer(CurrentAuth.require(principal).userId(), id);
		return ResponseEntity.ok(new EssaiResponse(essai.succes(), essai.message()));
	}

	private static ConnexionResponse toResponse(ConnexionModele connexion) {
		return new ConnexionResponse(connexion.id(), connexion.fournisseur(), connexion.modele());
	}

	public record CreateConnexionRequest(String provider, String model, String apiKey) {
	}

	public record ConnexionResponse(long id, String provider, String model) {
	}

	public record ConnexionListResponse(List<ConnexionResponse> connections) {
	}

	public record EssaiResponse(boolean success, String message) {
	}
}
