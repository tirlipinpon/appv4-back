package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ParentProfileUseCase;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.result.ParentProfileView;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Fiche du parent connecté (SCRUM-200).
 *
 * <p><b>Business :</b> il corrige son nom, son mail de contact, son téléphone et son avatar.
 * Le mail de connexion ne se change pas ici. Un professeur sans rôle parent n'ouvre pas cette fiche.
 * <p><b>Technique :</b> {@code GET} et {@code PUT /api/int/parent/profile}, JWT obligatoire.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link ParentProfileUseCase}. Les écoles
 * du professeur ne sont ni lues ni écrites.
 */
@RestController
@RequestMapping("/api/int/parent/profile")
@Tag(name = "Parent — profil")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class ParentProfileController {

	private final ParentProfileUseCase parentProfileUseCase;

	public ParentProfileController(ParentProfileUseCase parentProfileUseCase) {
		this.parentProfileUseCase = parentProfileUseCase;
	}

	@GetMapping
	@Operation(operationId = "getParentProfile", summary = "Lire la fiche du parent connecté")
	@ApiResponse(responseCode = "200", description = "Mail de connexion, nom, contact, téléphone, avatar et choix")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ParentProfileResponse> getProfile(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(toResponse(parentProfileUseCase.load(user.userId(), user.activeRole())));
	}

	@PutMapping
	@Operation(operationId = "updateParentProfile", summary = "Enregistrer la fiche du parent connecté")
	@ApiResponse(responseCode = "200", description = "Fiche enregistrée")
	@ApiResponse(
			responseCode = "400",
			description = "Nom, mail de contact, téléphone ou avatar refusé",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ParentProfileResponse> updateProfile(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestBody UpdateParentProfileRequest request) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		UpdateParentProfileRequest body = request == null
				? new UpdateParentProfileRequest(null, null, null, null)
				: request;
		ParentProfileView view = parentProfileUseCase.update(
				user.userId(),
				user.activeRole(),
				body.displayName(),
				body.contactEmail(),
				body.phone(),
				body.avatarCode());
		return ResponseEntity.ok(toResponse(view));
	}

	private static ParentProfileResponse toResponse(ParentProfileView view) {
		List<AvatarChoiceResponse> choices = view.avatarChoices().stream()
				.map(choice -> new AvatarChoiceResponse(choice.code(), choice.label()))
				.toList();
		return new ParentProfileResponse(
				view.email(),
				view.displayName(),
				view.contactEmail(),
				view.phone(),
				view.avatarCode(),
				choices);
	}

	@Schema(name = "ParentProfileResponse")
	public record ParentProfileResponse(
			String email,
			String displayName,
			@Schema(nullable = true) String contactEmail,
			@Schema(nullable = true) String phone,
			@Schema(nullable = true) String avatarCode,
			List<AvatarChoiceResponse> avatarChoices) {
	}

	@Schema(name = "UpdateParentProfileRequest")
	public record UpdateParentProfileRequest(
			String displayName,
			@Schema(nullable = true) String contactEmail,
			@Schema(nullable = true) String phone,
			@Schema(nullable = true) String avatarCode) {
	}
}
