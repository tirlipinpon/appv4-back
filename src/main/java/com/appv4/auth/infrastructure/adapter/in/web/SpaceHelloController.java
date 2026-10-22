package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.service.SpaceHello;
import com.appv4.configuration.OpenApiConfiguration;
import com.appv4.hello.infrastructure.adapter.in.web.HelloMessageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Portes de test des deux espaces (SCRUM-127).
 *
 * <p><b>Business :</b> un jeton professeur n'ouvre pas l'espace parent, et l'inverse.
 * Le hello général {@code /api/int/hello} ne passe pas par ici.
 * <p><b>Technique :</b> GET {@code /api/int/parent/hello} et {@code /api/int/teacher/hello}.
 * 403 si {@code activeRole} manque ou ne correspond pas. Aucune écriture en base.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link SpaceHello}.
 */
@RestController
@RequestMapping("/api/int")
@Tag(name = "Espaces — hello")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class SpaceHelloController {

	@GetMapping("/parent/hello")
	@Operation(operationId = "getParentHello", summary = "Hello de l'espace parent")
	@ApiResponse(responseCode = "200", description = "Jeton parent")
	@ApiResponse(
			responseCode = "403",
			description = "Jeton professeur, ou espace non choisi",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<HelloMessageResponse> getParentHello() {
		return ResponseEntity.ok(new HelloMessageResponse(SpaceHello.parentHello(currentActiveRole())));
	}

	@GetMapping("/teacher/hello")
	@Operation(operationId = "getTeacherHello", summary = "Hello de l'espace professeur")
	@ApiResponse(responseCode = "200", description = "Jeton professeur")
	@ApiResponse(
			responseCode = "403",
			description = "Jeton parent, ou espace non choisi",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<HelloMessageResponse> getTeacherHello() {
		return ResponseEntity.ok(new HelloMessageResponse(SpaceHello.teacherHello(currentActiveRole())));
	}

	private static String currentActiveRole() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return user.activeRole();
		}
		throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "Connexion requise.");
	}
}
