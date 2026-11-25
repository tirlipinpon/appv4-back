package com.appv4.auth.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ReadParentProgressUseCase;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.FailedEssai;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.FormeRatee;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.MissionDay;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.ParentProgress;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.ProgressDay;
import com.appv4.auth.application.port.in.ReadParentProgressUseCase.RecentSuccess;
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

	/**
	 * Lecture du suivi.
	 *
	 * <p><b>Business :</b> le parent qui a la fiche reçoit les quatre blocs.
	 * <p><b>Technique :</b> 200, 403 ou 404. Les nuls restent dans le JSON.
	 * <p><b>Métier / flow :</b> JWT → {@link ReadParentProgressUseCase}.
	 */
	@GetMapping
	@Operation(operationId = "readParentProgress", summary = "Lire la photo, les jours d'XP, la mission et les quatre lectures")
	@ApiResponse(responseCode = "200", description = "Photo, jours, mission du jour, réussite, et les quatre blocs déjà connus")
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
				progress.repeatedErrorPatterns().stream().map(ParentProgressController::toForme).toList(),
				progress.xpTotal(),
				progress.currentStreak(),
				progress.leagueLabel(),
				toSuccess(progress.recentSuccess()),
				progress.days().stream().map(ParentProgressController::toDay).toList(),
				toMission(progress.dailyMissionSummary()),
				progress.dailyMissionDays().stream().map(ParentProgressController::toMission).toList());
	}

	private static MissionDayResponse toMission(MissionDay day) {
		if (day == null) {
			return null;
		}
		List<FailedEssaiResponse> failed = null;
		if (day.failedEssais() != null) {
			failed = day.failedEssais().stream().map(ParentProgressController::toFailed).toList();
		}
		return new MissionDayResponse(
				day.date(), day.objective(), day.progress(), day.target(), day.status(), day.statusLabel(), failed);
	}

	private static FailedEssaiResponse toFailed(FailedEssai essai) {
		return new FailedEssaiResponse(essai.subjectLabel(), essai.prompt(), essai.at());
	}

	private static RecentSuccessResponse toSuccess(RecentSuccess success) {
		if (success == null) {
			return null;
		}
		return new RecentSuccessResponse(success.successRate(), success.attemptCount(), success.windowLabel());
	}

	private static ProgressDayResponse toDay(ProgressDay day) {
		return new ProgressDayResponse(day.date(), day.xpGained(), day.xpTotal(), day.passed(), day.failed());
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
			List<FormeResponse> repeatedErrorPatterns,
			int xpTotal,
			int currentStreak,
			String leagueLabel,
			RecentSuccessResponse recentSuccess,
			List<ProgressDayResponse> days,
			MissionDayResponse dailyMissionSummary,
			List<MissionDayResponse> dailyMissionDays) {
	}

	public record MissionDayResponse(
			LocalDate date,
			String objective,
			Integer progress,
			Integer target,
			String status,
			String statusLabel,
			List<FailedEssaiResponse> failedEssais) {
	}

	public record FailedEssaiResponse(String subjectLabel, String prompt, Instant at) {
	}

	public record RecentSuccessResponse(BigDecimal successRate, int attemptCount, String windowLabel) {
	}

	public record ProgressDayResponse(
			LocalDate date, int xpGained, int xpTotal, Integer passed, Integer failed) {
	}

	public record TrendResponse(String direction, BigDecimal deltaSuccessPoints, String windowLabel) {
	}

	public record FormeResponse(String formCode, int count, String label) {
	}
}
