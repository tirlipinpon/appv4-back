package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.CompleteRolesUseCase;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.result.JwtTokenResult;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Choix de rôle après la création du compte (SCRUM-153).
 *
 * <p><b>Business :</b> la même question pour l'email, Google et Facebook.
 * <p><b>Technique :</b> {@code POST /api/int/auth/complete-roles}, JWT obligatoire.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link CompleteRolesUseCase} → nouveau JWT.
 */
@RestController
@RequestMapping("/api/int/auth/complete-roles")
@Tag(name = "Auth — rôle à la création")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class CompleteRolesController {

	private final CompleteRolesUseCase completeRolesUseCase;

	public CompleteRolesController(CompleteRolesUseCase completeRolesUseCase) {
		this.completeRolesUseCase = completeRolesUseCase;
	}

	@PostMapping
	@Operation(operationId = "completeRoles", summary = "Choisir Parent ou Professeur après la création du compte")
	@ApiResponse(responseCode = "200", description = "JWT à jour")
	@ApiResponse(
			responseCode = "400",
			description = "Aucun rôle, ou rôle interdit",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<JwtTokenResponse> completeRoles(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody CompleteRolesRequest request) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		JwtTokenResult result = completeRolesUseCase.complete(user.userId(), user.email(), request.roles());
		return ResponseEntity.ok(new JwtTokenResponse(result.token(), result.expiresAtEpochSeconds()));
	}

	@Schema(name = "CompleteRolesRequest")
	public record CompleteRolesRequest(@NotEmpty List<@NotBlank String> roles) {
	}
}
