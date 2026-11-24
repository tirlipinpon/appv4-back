package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.SejourUseCase;
import com.appv4.auth.application.port.in.SejourUseCase.Journee;
import com.appv4.auth.application.port.in.SejourUseCase.JourneeLine;
import com.appv4.auth.application.port.in.SejourUseCase.JourneeMonth;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Lecture parent du mois d'un enfant (BACK-38, journée BACK-83).
 *
 * <p><b>Business :</b> la page Temps lit un mois. Chaque jour actif est une journée.
 * Les dates, les durées et les codes d'espace sont déjà décidés.
 * <p><b>Technique :</b> {@code GET /api/int/parent/children/{childId}/sejours}.
 * {@code year} et {@code month} vides veulent dire le mois de l'horloge.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link SejourUseCase#readParentMonth}.
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
	@Operation(operationId = "listParentSejours", summary = "Lire le mois d'un enfant, une journée par jour")
	@ApiResponse(responseCode = "200", description = "Mois demandé, journées de la plus récente à la plus ancienne")
	@ApiResponse(
			responseCode = "400",
			description = "Mois futur ou mois inconnu",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Cet enfant est introuvable",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<JourneeMonthResponse> list(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long childId,
			@RequestParam(required = false) Integer year,
			@RequestParam(required = false) Integer month) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(toResponse(
				sejourUseCase.readParentMonth(user.userId(), user.activeRole(), childId, year, month)));
	}

	static JourneeMonthResponse toResponse(JourneeMonth month) {
		List<JourneeItem> journees = month.journees().stream().map(ParentSejourController::toJournee).toList();
		return new JourneeMonthResponse(
				month.monthLabel(), month.year(), month.month(), month.currentMonth(), journees);
	}

	private static JourneeItem toJournee(Journee journee) {
		List<JourneeLineItem> lines = journee.lines().stream().map(ParentSejourController::toLine).toList();
		return new JourneeItem(
				journee.year(),
				journee.month(),
				journee.day(),
				journee.today(),
				journee.dateLabel(),
				journee.durationLabel(),
				journee.durationSeconds(),
				lines);
	}

	private static JourneeLineItem toLine(JourneeLine line) {
		return new JourneeLineItem(
				line.space(), line.label(), line.subjectName(), line.durationLabel(), line.durationSeconds());
	}

	/**
	 * @param space code d'écran, ou {@code question}
	 * @param label libellé déjà choisi, ou énoncé copié
	 * @param subjectName nom de matière copié, absent pour un écran
	 */
	@Schema(name = "JourneeLineItem")
	public record JourneeLineItem(
			String space, String label, String subjectName, String durationLabel, long durationSeconds) {
	}

	@Schema(name = "JourneeItem")
	public record JourneeItem(
			int year,
			int month,
			int day,
			boolean today,
			String dateLabel,
			String durationLabel,
			long durationSeconds,
			List<JourneeLineItem> lines) {
	}

	@Schema(name = "JourneeMonthResponse")
	public record JourneeMonthResponse(
			String monthLabel, int year, int month, boolean currentMonth, List<JourneeItem> journees) {
	}
}
