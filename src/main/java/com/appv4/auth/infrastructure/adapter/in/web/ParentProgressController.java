package com.appv4.auth.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ReadParentProgressUseCase;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.FormeRatee;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.ParentProgress;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.Trend;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Quatre lectures du suivi d'un enfant (BACK-40).
 *
 * <p><b>Business :</b> le parent lit le créneau, les minutes, la tendance et les formes ratées.
 * <p><b>Technique :</b> {@code GET /api/int/parent/children/{childId}/progress}.
 * Un bloc absent reste nul. La liste vide est une liste.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link ReadParentProgressUseCase}.
 */
@RestController
@RequestMapping("/api/int/parent/children/{childId}/progress")
@Tag(name = "Parent — suivi")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class ParentProgressController {

	private final ReadParentProgressUseCase readParentProgressUseCase;

	public ParentProgressController(ReadParentProgressUseCase readParentProgressUseCase) {
		this.readParentProgressUseCase = readParentProgressUseCase;
	}

	@GetMapping
	@Operation(operationId = "readParentProgress", summary = "Lire le créneau, les minutes, la tendance et les formes ratées")
	@ApiResponse(responseCode = "200", description = "Quatre blocs, absents quand il n'y a pas de donnée")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Cet enfant est introuvable",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ParentProgressResponse> read(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long childId) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(toResponse(readParentProgressUseCase.read(user.userId(), user.activeRole(), childId)));
	}

	private static ParentProgressResponse toResponse(ParentProgress progress) {
		return new ParentProgressResponse(
				progress.bestTimeSlotLabel(),
				progress.averageSessionMinutes(),
				toTrend(progress.trend()),
				progress.repeatedErrorPatterns().stream().map(ParentProgressController::toForme).toList());
	}

	private static TrendResponse toTrend(Trend trend) {
		if (trend == null) {
			return null;
		}
		return new TrendResponse(trend.direction(), trend.deltaSuccessPoints(), trend.windowLabel());
	}

	private static FormeResponse toForme(FormeRatee forme) {
		return new FormeResponse(forme.formCode(), forme.count(), forme.label());
	}

	public record ParentProgressResponse(
			String bestTimeSlotLabel,
			Integer averageSessionMinutes,
			TrendResponse trend,
			List<FormeResponse> repeatedErrorPatterns) {
	}

	public record TrendResponse(String direction, BigDecimal deltaSuccessPoints, String windowLabel) {
	}

	public record FormeResponse(String formCode, int count, String label) {
	}
}
