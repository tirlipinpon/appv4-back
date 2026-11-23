package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.SejourUseCase;
import com.appv4.auth.application.port.in.SejourUseCase.Line;
import com.appv4.auth.application.port.in.SejourUseCase.ParentSejour;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Lecture parent des séjours d'un enfant (BACK-38, question BACK-79).
 *
 * <p><b>Business :</b> la page Temps lit cette route. Les dates et les durées
 * sont déjà libellées. Une question montre l'énoncé et la matière copiés.
 * Un enfant hors Mes enfants est introuvable.
 * <p><b>Technique :</b> {@code GET /api/int/parent/children/{childId}/sejours}.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link SejourUseCase}.
 */
@RestController
@RequestMapping("/api/int/parent/children/{childId}/sejours")
@Tag(name = "Parent — séjours")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class ParentSejourController {

	private final SejourUseCase sejourUseCase;

	public ParentSejourController(SejourUseCase sejourUseCase) {
		this.sejourUseCase = sejourUseCase;
	}

	@GetMapping
	@Operation(operationId = "listParentSejours", summary = "Lire les séjours d'un enfant")
	@ApiResponse(responseCode = "200", description = "Séjours du plus récent au plus ancien, ou liste vide")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Cet enfant est introuvable",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ParentSejourListResponse> list(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long childId) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		List<ParentSejourItem> sejours = sejourUseCase.readParent(user.userId(), user.activeRole(), childId).stream()
				.map(ParentSejourController::toItem)
				.toList();
		return ResponseEntity.ok(new ParentSejourListResponse(sejours));
	}

	private static ParentSejourItem toItem(ParentSejour sejour) {
		List<ParentSejourLineItem> lines = sejour.lines().stream()
				.map(ParentSejourController::toLine)
				.toList();
		return new ParentSejourItem(sejour.dateLabel(), sejour.timeLabel(), sejour.durationLabel(), lines);
	}

	private static ParentSejourLineItem toLine(Line line) {
		return new ParentSejourLineItem(line.label(), line.subjectName(), line.durationLabel());
	}

	/**
	 * @param label libellé d'écran, ou énoncé copié pour une question
	 * @param subjectName nom de matière copié, absent pour un écran
	 */
	@Schema(name = "ParentSejourLineItem")
	public record ParentSejourLineItem(String label, String subjectName, String durationLabel) {
	}

	@Schema(name = "ParentSejourItem")
	public record ParentSejourItem(
			String dateLabel, String timeLabel, String durationLabel, List<ParentSejourLineItem> lines) {
	}

	@Schema(name = "ParentSejourListResponse")
	public record ParentSejourListResponse(List<ParentSejourItem> sejours) {
	}
}
