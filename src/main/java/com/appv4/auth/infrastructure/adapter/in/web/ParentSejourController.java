package com.appv4.auth.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.SejourUseCase;
import com.appv4.auth.application.port.in.SejourUseCase.Journee;
import com.appv4.auth.application.port.in.SejourUseCase.JourneeLine;
import com.appv4.auth.application.port.in.SejourUseCase.JourneeMonth;
import com.appv4.auth.application.port.in.SejourUseCase.Plafond;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Lecture parent du mois d'un enfant, et plafond du référent (BACK-38, BACK-83, BACK-42).
 *
 * <p><b>Business :</b> la page Temps lit un mois et le plafond d'aujourd'hui.
 * Le parent référent pose ou retire ce plafond. Les dates et les durées sont déjà décidées.
 * <p><b>Technique :</b> {@code GET} et {@code PUT .../plafond} sur
 * {@code /api/int/parent/children/{childId}/sejours}.
 * {@code year} et {@code month} vides veulent dire le mois de l'horloge.
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
	@Operation(operationId = "listParentSejours", summary = "Lire le mois d'un enfant, une journée par jour")
	@ApiResponse(responseCode = "200", description = "Mois demandé, journées, et plafond comparé à aujourd'hui")
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
	public ResponseEntity<ParentJourneeMonthResponse> list(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long childId,
			@RequestParam(required = false) Integer year,
			@RequestParam(required = false) Integer month) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(toParentResponse(
				sejourUseCase.readParentMonth(user.userId(), user.activeRole(), childId, year, month)));
	}

	@PutMapping("/plafond")
	@Operation(operationId = "saveParentPlafond", summary = "Poser ou retirer le plafond du jour")
	@ApiResponse(responseCode = "200", description = "Plafond enregistré, déjà comparé à aujourd'hui")
	@ApiResponse(
			responseCode = "400",
			description = "Nombre hors de 1 à 960, ou parent qui n'est pas le référent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Cet enfant est introuvable",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<PlafondResponse> save(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long childId,
			@RequestBody PlafondRequest body) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(toPlafond(sejourUseCase.saveParentPlafond(
				user.userId(), user.activeRole(), childId, body.limitMinutes())));
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

	@Schema(name = "ParentJourneeMonthResponse")
	public record ParentJourneeMonthResponse(
			String monthLabel,
			int year,
			int month,
			boolean currentMonth,
			List<JourneeItem> journees,
			PlafondResponse plafond) {
	}

	/**
	 * @param limitMinutes minutes posées, vide s'il n'y a pas de plafond
	 * @param message phrase exacte, ou vide
	 */
	@Schema(name = "PlafondResponse")
	public record PlafondResponse(Integer limitMinutes, boolean exceeded, String message, boolean editable) {
	}

	/** @param limitMinutes entier de 1 à 960, ou vide pour retirer le plafond */
	@Schema(name = "PlafondRequest")
	public record PlafondRequest(BigDecimal limitMinutes) {
	}

	private static ParentJourneeMonthResponse toParentResponse(JourneeMonth month) {
		List<JourneeItem> journees = month.journees().stream().map(ParentSejourController::toJournee).toList();
		return new ParentJourneeMonthResponse(
				month.monthLabel(),
				month.year(),
				month.month(),
				month.currentMonth(),
				journees,
				toPlafond(month.plafond()));
	}

	private static PlafondResponse toPlafond(Plafond plafond) {
		return new PlafondResponse(
				plafond.limitMinutes(), plafond.exceeded(), plafond.message(), plafond.editable());
	}
}
