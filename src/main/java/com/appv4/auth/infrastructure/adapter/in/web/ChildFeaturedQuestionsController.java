package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ReadChildFeaturedQuestionsUseCase;
import com.appv4.auth.application.result.ChildFeaturedQuestionsView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Questions proposées sur l'accueil de l'enfant (BACK-20).
 *
 * <p><b>Business :</b> l'accueil affiche au plus trois énoncés, sans les choisir.
 * <p><b>Technique :</b> {@code GET /api/ext/featured}, Bearer enfant.
 * <p><b>Métier / flow :</b> adapter in → {@link ReadChildFeaturedQuestionsUseCase}.
 */
@RestController
@RequestMapping("/api/ext/featured")
@Tag(name = "Enfant — accueil")
public class ChildFeaturedQuestionsController {

	private final ReadChildFeaturedQuestionsUseCase readChildFeaturedQuestionsUseCase;

	public ChildFeaturedQuestionsController(ReadChildFeaturedQuestionsUseCase readChildFeaturedQuestionsUseCase) {
		this.readChildFeaturedQuestionsUseCase = readChildFeaturedQuestionsUseCase;
	}

	@GetMapping
	@Operation(operationId = "readChildFeaturedQuestions", summary = "Lire les questions proposées à l'accueil")
	@ApiResponse(
			responseCode = "200",
			description = "Au plus trois énoncés, ou une liste vide",
			content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChildFeaturedQuestionItem.class))))
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<List<ChildFeaturedQuestionItem>> featured(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		ChildFeaturedQuestionsView view = readChildFeaturedQuestionsUseCase.read(bearer(authorization));
		List<ChildFeaturedQuestionItem> items = view.questions().stream()
				.map(question -> new ChildFeaturedQuestionItem(
						question.subjectId(), question.questionId(), question.prompt()))
				.toList();
		return ResponseEntity.ok(items);
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}

	@Schema(name = "ChildFeaturedQuestionItem")
	public record ChildFeaturedQuestionItem(long subjectId, long questionId, String prompt) {
	}
}
