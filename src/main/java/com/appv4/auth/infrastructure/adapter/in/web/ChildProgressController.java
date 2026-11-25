package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ReadChildProgressUseCase;
import com.appv4.auth.application.result.ChildProgressView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Progression de l'enfant connecté (BACK-17).
 *
 * <p><b>Business :</b> home lit XP, streak, palier et la grille de la semaine.
 * <p><b>Technique :</b> {@code GET /api/ext/progress} avec Bearer enfant.
 * <p><b>Métier / flow :</b> adapter in → {@link ReadChildProgressUseCase}.
 */
@RestController
@RequestMapping("/api/ext/progress")
@Tag(name = "Enfant — progression")
public class ChildProgressController {

	private final ReadChildProgressUseCase readChildProgressUseCase;

	public ChildProgressController(ReadChildProgressUseCase readChildProgressUseCase) {
		this.readChildProgressUseCase = readChildProgressUseCase;
	}

	@GetMapping
	@Operation(operationId = "readChildProgress", summary = "Lire XP, streak, palier et semaine")
	@ApiResponse(responseCode = "200", description = "Progression de la fiche active")
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildProgressResponse> progress(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		ChildProgressView view = readChildProgressUseCase.progress(bearer(authorization));
		return ResponseEntity.ok(new ChildProgressResponse(
				view.xpTotal(), view.currentStreak(), view.leagueLabel(), view.weekDays(), view.weekYellowCounts()));
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}

	@Schema(name = "ChildProgressResponse")
	public record ChildProgressResponse(
			int xpTotal,
			int currentStreak,
			String leagueLabel,
			List<Boolean> weekDays,
			@Schema(description = "Étoiles jaunes du lundi au dimanche. Null pour un jour encore à venir.")
			List<Integer> weekYellowCounts) {
	}
}
