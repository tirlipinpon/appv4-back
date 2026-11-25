package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.AppHoursUseCase;
import com.appv4.auth.application.port.in.AppHoursUseCase.AppHoursView;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Horaires de l'app enfant (admin seulement).
 *
 * <p><b>Business :</b> l'admin lit et change l'heure de début et l'heure de fin.
 * Un parent ou un professeur n'écrit pas ici.
 * <p><b>Technique :</b> {@code /api/int/admin/app-hours}, JWT. La phrase de
 * fermeture est celle du cas d'usage.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link AppHoursUseCase}.
 */
@RestController
@RequestMapping("/api/int/admin/app-hours")
@Tag(name = "Admin — horaires")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class AppHoursController {

	private final AppHoursUseCase appHoursUseCase;

	public AppHoursController(AppHoursUseCase appHoursUseCase) {
		this.appHoursUseCase = appHoursUseCase;
	}

	@GetMapping
	@Operation(operationId = "readAppHours", summary = "Lire les horaires de l'app enfant")
	@ApiResponse(responseCode = "200", description = "Heure de début, heure de fin, phrase de fermeture")
	@ApiResponse(
			responseCode = "403",
			description = "Le compte n'est pas admin",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<AppHoursResponse> read(@AuthenticationPrincipal AuthenticatedUser principal) {
		return ResponseEntity.ok(toResponse(appHoursUseCase.read(CurrentAuth.require(principal).userId())));
	}

	@PutMapping
	@Operation(operationId = "updateAppHours", summary = "Changer les horaires de l'app enfant")
	@ApiResponse(responseCode = "200", description = "Horaires enregistrés")
	@ApiResponse(
			responseCode = "400",
			description = "Heure illisible, ou début qui n'est pas avant la fin",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le compte n'est pas admin",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<AppHoursResponse> update(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestBody AppHoursRequest request) {
		AppHoursRequest body = request == null ? new AppHoursRequest(null, null) : request;
		AppHoursView saved = appHoursUseCase.update(
				CurrentAuth.require(principal).userId(), body.opensAt(), body.closesAt());
		return ResponseEntity.ok(toResponse(saved));
	}

	private static AppHoursResponse toResponse(AppHoursView view) {
		return new AppHoursResponse(view.opensAt(), view.closesAt(), view.closedMessage());
	}

	@Schema(name = "AppHoursRequest")
	public record AppHoursRequest(String opensAt, String closesAt) {
	}

	@Schema(name = "AppHoursResponse")
	public record AppHoursResponse(String opensAt, String closesAt, String closedMessage) {
	}
}
