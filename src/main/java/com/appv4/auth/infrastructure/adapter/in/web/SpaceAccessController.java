package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.AddMissingSpaceUseCase;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.result.JwtTokenResult;
import com.appv4.auth.application.result.SpaceAccess;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Ajouter plus tard l'espace parent ou la demande professeur (SCRUM-152).
 *
 * <p><b>Business :</b> une personne déjà connectée complète son compte sans
 * changer d'email et sans ouvrir un second compte.
 * <p><b>Technique :</b> {@code /api/int/auth/spaces}, JWT obligatoire.
 * Le POST teacher ne renvoie pas de jeton.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link AddMissingSpaceUseCase}.
 * Le changement d'espace ({@code switch-role}) reste un autre chemin : il ne crée rien.
 */
@RestController
@RequestMapping("/api/int/auth/spaces")
@Tag(name = "Auth — espace manquant")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class SpaceAccessController {

	private final AddMissingSpaceUseCase addMissingSpaceUseCase;

	public SpaceAccessController(AddMissingSpaceUseCase addMissingSpaceUseCase) {
		this.addMissingSpaceUseCase = addMissingSpaceUseCase;
	}

	@GetMapping
	@Operation(
			operationId = "getSpaceAccess",
			summary = "Rôles déjà accordés et état de la demande professeur")
	@ApiResponse(responseCode = "200", description = "État pour l'en-tête")
	public ResponseEntity<SpaceAccessResponse> view(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(toResponse(addMissingSpaceUseCase.view(user.userId())));
	}

	@PostMapping("/parent")
	@Operation(
			operationId = "openParentSpace",
			summary = "Ouvrir l'espace parent tout de suite et recevoir un jeton à jour")
	@ApiResponse(responseCode = "200", description = "JWT avec le rôle parent")
	public ResponseEntity<JwtTokenResponse> openParent(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		JwtTokenResult result = addMissingSpaceUseCase.openParent(user.userId(), user.email());
		return ResponseEntity.ok(new JwtTokenResponse(result.token(), result.expiresAtEpochSeconds()));
	}

	@PostMapping("/teacher")
	@Operation(
			operationId = "requestTeacherSpace",
			summary = "Demander l'espace professeur sans écrire le rôle teacher")
	@ApiResponse(responseCode = "200", description = "Demande pending, ou état inchangé si déjà demandé ou déjà teacher")
	public ResponseEntity<SpaceAccessResponse> requestTeacher(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(toResponse(addMissingSpaceUseCase.requestTeacher(user.userId())));
	}

	private static SpaceAccessResponse toResponse(SpaceAccess access) {
		return new SpaceAccessResponse(access.roles(), access.teacherRequest());
	}
}
