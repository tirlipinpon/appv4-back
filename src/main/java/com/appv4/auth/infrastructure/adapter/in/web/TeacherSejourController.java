package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.SejourUseCase;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.infrastructure.adapter.in.web.ParentSejourController.JourneeMonthResponse;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Lecture professeur du mois d'un élève (BACK-82, journée BACK-83).
 *
 * <p><b>Business :</b> le professeur ouvre un élève de sa classe et voit les mêmes
 * journées que le parent. Pas de plafond ici. Un élève hors classe est refusé
 * avec la phrase déjà connue.
 * <p><b>Technique :</b> {@code GET /api/int/teacher/students/{childId}/sejours}.
 * Le corps reprend les journées. Le plafond du parent n'est pas copié.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link SejourUseCase#readTeacherMonth}.
 */
@RestController
@RequestMapping("/api/int/teacher/students/{childId}/sejours")
@Tag(name = "Professeur — séjours")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class TeacherSejourController {

	private final SejourUseCase sejourUseCase;

	public TeacherSejourController(SejourUseCase sejourUseCase) {
		this.sejourUseCase = sejourUseCase;
	}

	@GetMapping
	@Operation(operationId = "listTeacherSejours", summary = "Lire le mois d'un élève, une journée par jour")
	@ApiResponse(responseCode = "200", description = "Même mois que la lecture parent")
	@ApiResponse(
			responseCode = "400",
			description = "Mois futur ou mois inconnu",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Cet élève n'est pas dans tes classes",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<JourneeMonthResponse> list(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long childId,
			@RequestParam(required = false) Integer year,
			@RequestParam(required = false) Integer month) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(ParentSejourController.toResponse(
				sejourUseCase.readTeacherMonth(user.userId(), user.activeRole(), childId, year, month)));
	}
}
