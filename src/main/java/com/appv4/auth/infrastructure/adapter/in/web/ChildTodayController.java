package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ReadChildTodayUseCase;
import com.appv4.auth.application.result.ChildTodayView;
import com.appv4.auth.application.result.ChildTodayView.Mission;
import com.appv4.auth.application.result.ChildTodayView.Task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Mission du jour de l'enfant connecté (BACK-18).
 *
 * <p><b>Business :</b> l'accueil lit l'objectif, les tâches et le lien Play.
 * <p><b>Technique :</b> {@code GET /api/ext/today} avec Bearer enfant.
 * <p><b>Métier / flow :</b> adapter in → {@link ReadChildTodayUseCase}.
 */
@RestController
@RequestMapping("/api/ext/today")
@Tag(name = "Enfant — mission du jour")
public class ChildTodayController {

	private final ReadChildTodayUseCase readChildTodayUseCase;

	public ChildTodayController(ReadChildTodayUseCase readChildTodayUseCase) {
		this.readChildTodayUseCase = readChildTodayUseCase;
	}

	@GetMapping
	@Operation(operationId = "readChildToday", summary = "Lire la mission et le plan du jour")
	@ApiResponse(responseCode = "200", description = "Mission du jour, ou mission absente")
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildTodayResponse> today(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		ChildTodayView view = readChildTodayUseCase.today(bearer(authorization));
		return ResponseEntity.ok(ChildTodayResponse.from(view));
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}

	@Schema(name = "ChildTodayResponse")
	public record ChildTodayResponse(@Schema(nullable = true) ChildTodayMission mission) {

		static ChildTodayResponse from(ChildTodayView view) {
			if (view.mission() == null) {
				return new ChildTodayResponse(null);
			}
			return new ChildTodayResponse(ChildTodayMission.from(view.mission()));
		}
	}

	@Schema(name = "ChildTodayMission")
	public record ChildTodayMission(
			long id,
			String objective,
			int progress,
			int target,
			int rewardXp,
			String status,
			String statusLabel,
			long secondsRemaining,
			@Schema(nullable = true) Long playSubjectId,
			@Schema(nullable = true) Long playQuestionId,
			boolean playable,
			List<ChildTodayTask> tasks) {

		static ChildTodayMission from(Mission mission) {
			return new ChildTodayMission(
					mission.id(),
					mission.objective(),
					mission.progress(),
					mission.target(),
					mission.rewardXp(),
					mission.status(),
					mission.statusLabel(),
					mission.secondsRemaining(),
					mission.playSubjectId(),
					mission.playQuestionId(),
					mission.playable(),
					mission.tasks().stream().map(ChildTodayTask::from).toList());
		}
	}

	@Schema(name = "ChildTodayTask")
	public record ChildTodayTask(
			long id,
			String label,
			int progress,
			int target,
			int rewardXp,
			String status,
			String statusLabel,
			long subjectId,
			long questionId,
			boolean playable) {

		static ChildTodayTask from(Task task) {
			return new ChildTodayTask(
					task.id(),
					task.label(),
					task.progress(),
					task.target(),
					task.rewardXp(),
					task.status(),
					task.statusLabel(),
					task.subjectId(),
					task.questionId(),
					task.playable());
		}
	}
}
