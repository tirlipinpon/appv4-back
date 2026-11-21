package com.appv4.auth.infrastructure.adapter.in.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ReadChildBadgesUseCase;
import com.appv4.auth.application.result.ChildBadgesView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Collection des badges de l'enfant (BACK-19).
 *
 * <p><b>Business :</b> Récompenses et l'accueil affichent cette réponse, sans la recalculer.
 * <p><b>Technique :</b> {@code GET /api/ext/badges} avec Bearer enfant.
 * <p><b>Métier / flow :</b> adapter in → {@link ReadChildBadgesUseCase}.
 */
@RestController
@RequestMapping("/api/ext/badges")
@Tag(name = "Enfant — badges")
public class ChildBadgesController {

	private final ReadChildBadgesUseCase readChildBadgesUseCase;

	public ChildBadgesController(ReadChildBadgesUseCase readChildBadgesUseCase) {
		this.readChildBadgesUseCase = readChildBadgesUseCase;
	}

	@GetMapping
	@Operation(operationId = "readChildBadges", summary = "Lire la collection de badges")
	@ApiResponse(responseCode = "200", description = "Neuf badges, bloc du jour, avertissement éventuel")
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildBadgesResponse> badges(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		ChildBadgesView view = readChildBadgesUseCase.badges(bearer(authorization));
		return ResponseEntity.ok(ChildBadgesResponse.from(view));
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}

	@Schema(name = "ChildBadgesResponse")
	public record ChildBadgesResponse(
			List<BadgeResponse> badges, List<GainedResponse> gainedToday, NoticeResponse notice) {

		static ChildBadgesResponse from(ChildBadgesView view) {
			ChildBadgesView.Notice notice = view.notice();
			return new ChildBadgesResponse(
					view.badges().stream().map(BadgeResponse::from).toList(),
					view.gainedToday().stream().map(GainedResponse::from).toList(),
					notice == null ? null : new NoticeResponse(
							notice.label(), notice.subjectName(), notice.remainingCount()));
		}
	}

	@Schema(name = "ChildBadgeNoticeResponse")
	public record NoticeResponse(String label, String subjectName, int remainingCount) {
	}

	@Schema(name = "ChildBadgeResponse")
	public record BadgeResponse(
			String code,
			String name,
			int level,
			int retainedValue,
			Integer nextThreshold,
			LocalDate unlockedOn,
			int progressValue) {

		static BadgeResponse from(ChildBadgesView.Badge badge) {
			return new BadgeResponse(
					badge.code(), badge.name(), badge.level(), badge.retainedValue(),
					badge.nextThreshold(), badge.unlockedOn(), badge.progressValue());
		}
	}

	@Schema(name = "ChildBadgeGainedResponse")
	public record GainedResponse(String code, String name, int level) {

		static GainedResponse from(ChildBadgesView.Gained gained) {
			return new GainedResponse(gained.code(), gained.name(), gained.level());
		}
	}
}
