package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.SwitchActiveRoleUseCase;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.result.JwtTokenResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Changement d'espace pour un compte déjà connecté (SCRUM-124).
 *
 * <p><b>Business :</b> passer de parent à professeur sans se déconnecter,
 * seulement si le rôle est déjà accordé.
 * <p><b>Technique :</b> {@code POST /api/int/auth/switch-role}, JWT obligatoire.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link SwitchActiveRoleUseCase}
 * → nouveau JWT.
 */
@RestController
@RequestMapping("/api/int/auth/switch-role")
@Tag(name = "Auth — rôle actif")
public class SwitchRoleController {

	private final SwitchActiveRoleUseCase switchActiveRoleUseCase;

	public SwitchRoleController(SwitchActiveRoleUseCase switchActiveRoleUseCase) {
		this.switchActiveRoleUseCase = switchActiveRoleUseCase;
	}

	@PostMapping
	@Operation(operationId = "switchRole", summary = "Changer l'espace actif et recevoir un nouveau jeton")
	@ApiResponse(responseCode = "200", description = "Nouveau JWT avec activeRole")
	@ApiResponse(
			responseCode = "403",
			description = "Le compte n'a pas ce rôle",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<JwtTokenResponse> switchRole(@Valid @RequestBody SwitchRoleRequest request) {
		AuthenticatedUser user = currentUser();
		JwtTokenResult result = switchActiveRoleUseCase.switchRole(user.userId(), user.email(), request.role());
		return ResponseEntity.ok(new JwtTokenResponse(result.token(), result.expiresAtEpochSeconds()));
	}

	private static AuthenticatedUser currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return user;
		}
		throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "Connexion requise.");
	}

	@Schema(name = "SwitchRoleRequest")
	public record SwitchRoleRequest(@NotBlank String role) {
	}
}
