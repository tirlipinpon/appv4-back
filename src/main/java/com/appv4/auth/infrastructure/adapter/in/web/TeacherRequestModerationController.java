package com.appv4.auth.infrastructure.adapter.in.web;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.TeacherModerationUseCase;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.port.out.TeacherRequestView;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * File de modération des demandes professeur (SCRUM-142).
 *
 * <p><b>Business :</b> l'admin voit qui attend, et peut dire oui.
 * <p><b>Technique :</b> {@code GET} et {@code POST /api/int/admin/teacher-requests}, JWT + rôle admin.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link TeacherModerationUseCase}.
 */
@RestController
@RequestMapping("/api/int/admin/teacher-requests")
@Tag(name = "Admin — demandes professeur")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class TeacherRequestModerationController {

	private final TeacherModerationUseCase teacherModerationUseCase;

	public TeacherRequestModerationController(TeacherModerationUseCase teacherModerationUseCase) {
		this.teacherModerationUseCase = teacherModerationUseCase;
	}

	@GetMapping
	@Operation(operationId = "listTeacherRequests", summary = "Lister les demandes professeur en attente et acceptées")
	@ApiResponse(responseCode = "200", description = "File de modération")
	@ApiResponse(
			responseCode = "403",
			description = "Le compte n'est pas admin",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherRequestListResponse> list() {
		List<TeacherRequestItem> requests = teacherModerationUseCase.list(currentUser().userId()).stream()
				.map(TeacherRequestModerationController::toItem)
				.toList();
		return ResponseEntity.ok(new TeacherRequestListResponse(requests));
	}

	@PostMapping("/{id}/approve")
	@Operation(operationId = "approveTeacherRequest", summary = "Accepter une demande professeur")
	@ApiResponse(responseCode = "200", description = "Demande acceptée")
	@ApiResponse(
			responseCode = "403",
			description = "Le compte n'est pas admin",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Demande inconnue",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherRequestItem> approve(@PathVariable long id) {
		return ResponseEntity.ok(toItem(teacherModerationUseCase.approve(currentUser().userId(), id)));
	}

	@PostMapping("/{id}/revoke")
	@Operation(operationId = "revokeTeacherRequest", summary = "Reprendre un rôle professeur déjà accepté")
	@ApiResponse(responseCode = "200", description = "Demande révoquée")
	@ApiResponse(
			responseCode = "403",
			description = "Le compte n'est pas admin",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Demande inconnue",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherRequestItem> revoke(@PathVariable long id) {
		return ResponseEntity.ok(toItem(teacherModerationUseCase.revoke(currentUser().userId(), id)));
	}

	private static TeacherRequestItem toItem(TeacherRequestView view) {
		return new TeacherRequestItem(
				view.id(),
				view.email(),
				view.displayName(),
				view.status(),
				view.createdAt(),
				view.reviewedAt(),
				view.revokedAt());
	}

	private static AuthenticatedUser currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return user;
		}
		throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "Connexion requise.");
	}

	@Schema(name = "TeacherRequestItem")
	public record TeacherRequestItem(
			long id,
			String email,
			String displayName,
			String status,
			Instant createdAt,
			Instant reviewedAt,
			Instant revokedAt) {
	}

	@Schema(name = "TeacherRequestListResponse")
	public record TeacherRequestListResponse(List<TeacherRequestItem> requests) {
	}
}
