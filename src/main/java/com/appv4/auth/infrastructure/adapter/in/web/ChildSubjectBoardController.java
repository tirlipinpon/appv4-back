package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ReadChildSubjectBoardUseCase;
import com.appv4.auth.application.result.ChildSubjectBoardView;
import com.appv4.auth.application.result.ChildSubjectBoardView.ContinueLesson;
import com.appv4.auth.application.result.ChildSubjectBoardView.TopicCard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Board matière de l'enfant connecté (BACK-33).
 *
 * <p><b>Business :</b> Learn ouvre le détail d'une matière liée.
 * <p><b>Technique :</b> {@code GET /api/ext/subjects/{id}} avec Bearer enfant.
 * <p><b>Métier / flow :</b> adapter in → {@link ReadChildSubjectBoardUseCase}.
 */
@RestController
@RequestMapping("/api/ext/subjects")
@Tag(name = "Enfant — board matière")
public class ChildSubjectBoardController {

	private final ReadChildSubjectBoardUseCase readChildSubjectBoardUseCase;

	public ChildSubjectBoardController(ReadChildSubjectBoardUseCase readChildSubjectBoardUseCase) {
		this.readChildSubjectBoardUseCase = readChildSubjectBoardUseCase;
	}

	@GetMapping("/{subjectId}")
	@Operation(operationId = "readChildSubjectBoard", summary = "Lire le board d'une matière liée")
	@ApiResponse(responseCode = "200", description = "Board de la matière")
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière absente ou non liée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildSubjectBoardResponse> board(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable long subjectId) {
		ChildSubjectBoardView view = readChildSubjectBoardUseCase.board(bearer(authorization), subjectId);
		return ResponseEntity.ok(ChildSubjectBoardResponse.from(view));
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}

	@Schema(name = "ChildSubjectBoardResponse")
	public record ChildSubjectBoardResponse(
			long subjectId,
			String name,
			String iconCode,
			String grade,
			@Schema(nullable = true) String description,
			@Schema(nullable = true) String tagline,
			@Schema(nullable = true) String worldLabel,
			@Schema(nullable = true) String ageLabel,
			@Schema(nullable = true) String levelLabel,
			int xpEarned,
			int badgeCount,
			int lessonsCompleted,
			int lessonsTotal,
			@Schema(nullable = true) Integer minutesLeft,
			@Schema(nullable = true) ChildSubjectContinueLesson continueLesson,
			List<ChildSubjectTopicCard> topics) {

		static ChildSubjectBoardResponse from(ChildSubjectBoardView view) {
			return new ChildSubjectBoardResponse(
					view.subjectId(),
					view.name(),
					view.iconCode(),
					view.grade(),
					view.description(),
					view.tagline(),
					view.worldLabel(),
					view.ageLabel(),
					view.levelLabel(),
					view.xpEarned(),
					view.badgeCount(),
					view.lessonsCompleted(),
					view.lessonsTotal(),
					view.minutesLeft(),
					view.continueLesson() == null ? null : ChildSubjectContinueLesson.from(view.continueLesson()),
					view.topics().stream().map(ChildSubjectTopicCard::from).toList());
		}
	}

	@Schema(name = "ChildSubjectContinueLesson")
	public record ChildSubjectContinueLesson(
			long lessonId,
			String title,
			int progressPercent,
			int minutesLeft,
			int rewardXp,
			long playQuestionId) {

		static ChildSubjectContinueLesson from(ContinueLesson lesson) {
			return new ChildSubjectContinueLesson(
					lesson.lessonId(),
					lesson.title(),
					lesson.progressPercent(),
					lesson.minutesLeft(),
					lesson.rewardXp(),
					lesson.playQuestionId());
		}
	}

	@Schema(name = "ChildSubjectTopicCard")
	public record ChildSubjectTopicCard(
			long topicId,
			String name,
			@Schema(nullable = true) String iconCode,
			int lessonsCompleted,
			int lessonsTotal) {

		static ChildSubjectTopicCard from(TopicCard topic) {
			return new ChildSubjectTopicCard(
					topic.topicId(),
					topic.name(),
					topic.iconCode(),
					topic.lessonsCompleted(),
					topic.lessonsTotal());
		}
	}
}
