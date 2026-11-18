package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ReadChildLearningSubjectsUseCase;
import com.appv4.auth.application.result.ChildLearningQuestionsView;
import com.appv4.auth.application.result.ChildLearningSubjectsView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Matières et questions de l'enfant connecté (BACK-15).
 *
 * <p><b>Business :</b> Learn affiche les matières liées et ouvre leurs questions.
 * <p><b>Technique :</b> {@code GET /api/ext/subjects}, Bearer enfant.
 * <p><b>Métier / flow :</b> adapter in → {@link ReadChildLearningSubjectsUseCase}.
 */
@RestController
@RequestMapping("/api/ext/subjects")
@Tag(name = "Enfant — matières")
public class ChildLearningSubjectsController {

	private final ReadChildLearningSubjectsUseCase readChildLearningSubjectsUseCase;

	public ChildLearningSubjectsController(ReadChildLearningSubjectsUseCase readChildLearningSubjectsUseCase) {
		this.readChildLearningSubjectsUseCase = readChildLearningSubjectsUseCase;
	}

	@GetMapping
	@Operation(operationId = "listChildLearningSubjects", summary = "Lister les matières de l'enfant connecté")
	@ApiResponse(responseCode = "200", description = "Matières liées, actives")
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildLearningSubjectsResponse> list(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		ChildLearningSubjectsView view = readChildLearningSubjectsUseCase.list(bearer(authorization));
		List<ChildLearningSubjectItem> items = view.subjects().stream()
				.map(s -> new ChildLearningSubjectItem(
						s.subjectId(), s.name(), s.grade(), s.questionCount(), s.iconCode()))
				.toList();
		return ResponseEntity.ok(new ChildLearningSubjectsResponse(items));
	}

	@GetMapping("/{subjectId}/questions")
	@Operation(operationId = "listChildLearningSubjectQuestions", summary = "Lister les questions d'une matière liée")
	@ApiResponse(responseCode = "200", description = "Questions en lecture seule")
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière absente ou non liée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildLearningQuestionsResponse> questions(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@PathVariable long subjectId) {
		ChildLearningQuestionsView view = readChildLearningSubjectsUseCase.questions(bearer(authorization), subjectId);
		List<ChildLearningQuestionItem> items = view.questions().stream()
				.map(q -> new ChildLearningQuestionItem(q.questionId(), q.formCode(), q.prompt()))
				.toList();
		return ResponseEntity.ok(new ChildLearningQuestionsResponse(view.subjectId(), view.subjectName(), items));
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}

	@Schema(name = "ChildLearningSubjectsResponse")
	public record ChildLearningSubjectsResponse(List<ChildLearningSubjectItem> subjects) {
	}

	@Schema(name = "ChildLearningSubjectItem")
	public record ChildLearningSubjectItem(
			long subjectId,
			String name,
			String grade,
			long questionCount,
			String iconCode) {
	}

	@Schema(name = "ChildLearningQuestionsResponse")
	public record ChildLearningQuestionsResponse(
			long subjectId,
			String subjectName,
			List<ChildLearningQuestionItem> questions) {
	}

	@Schema(name = "ChildLearningQuestionItem")
	public record ChildLearningQuestionItem(long questionId, String formCode, String prompt) {
	}
}
