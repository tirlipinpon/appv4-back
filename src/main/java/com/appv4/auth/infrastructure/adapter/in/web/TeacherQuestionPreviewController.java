package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.PreviewVerdict;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.service.question.QuestionFormRegistry;
import com.appv4.auth.infrastructure.adapter.in.web.ChildQuestionPlayController.ChildLetterLocksRequest;
import com.appv4.auth.infrastructure.adapter.in.web.ChildQuestionPlayController.ChildLetterLocksResponse;
import com.appv4.auth.infrastructure.adapter.in.web.ChildQuestionPlayController.MemoryPairResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Simulation professeur sur une question de sa matière, sans écriture de progression. */
@RestController
@RequestMapping("/api/int/teacher/subjects/{subjectId}/questions/{questionId}/preview")
@Tag(name = "Professeur — aperçu")
public class TeacherQuestionPreviewController {
	private final TeacherQuestionUseCase questions;
	private final QuestionFormRegistry forms;

	public TeacherQuestionPreviewController(TeacherQuestionUseCase questions, QuestionFormRegistry forms) {
		this.questions = questions;
		this.forms = forms;
	}

	@GetMapping
	@Operation(operationId = "getTeacherQuestionPreview", summary = "Ouvrir un aperçu professeur")
	public ResponseEntity<PlayQuestionResponse> preview(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable long subjectId,
			@PathVariable long questionId) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		int maxAttempts = questions.previewMaxAttempts(user.userId(), user.activeRole(), subjectId, questionId);
		return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(PlayQuestionResponse.from(
				questions.preview(user.userId(), user.activeRole(), subjectId, questionId), forms,
				maxAttempts, maxAttempts));
	}

	@PostMapping("/attempts")
	@Operation(operationId = "gradeTeacherQuestionPreview", summary = "Corriger une réponse en simulation")
	public ResponseEntity<PreviewVerdict> grade(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable long subjectId,
			@PathVariable long questionId, @Valid @RequestBody PreviewAttemptRequest body) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(questions.previewAnswer(
				user.userId(), user.activeRole(), subjectId, questionId,
				body.answer().toPreviewSubmission(), body.previewSequence()));
	}

	@PostMapping("/letter-locks")
	@Operation(operationId = "checkTeacherPreviewLetterLocks", summary = "Vérifier des lettres en simulation")
	public ResponseEntity<ChildLetterLocksResponse> letterLocks(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable long subjectId,
			@PathVariable long questionId, @Valid @RequestBody ChildLetterLocksRequest body) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(new ChildLetterLocksResponse(questions.previewLetterLocks(
				user.userId(), user.activeRole(), subjectId, questionId, body.attempt()).locks()));
	}

	@PostMapping("/memory-pair")
	@Operation(operationId = "checkTeacherPreviewMemoryPair", summary = "Vérifier deux cartes en simulation")
	public ResponseEntity<MemoryPairResponse> memoryPair(
			@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable long subjectId,
			@PathVariable long questionId, @Valid @RequestBody PlayAttemptRequest.SubmitMemoryMatch body) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(new MemoryPairResponse(questions.previewMemoryPair(
				user.userId(), user.activeRole(), subjectId, questionId, body.match())));
	}

	public record PreviewAttemptRequest(
			@NotNull @Valid PlayAttemptRequest answer,
			@Size(max = 8) List<@NotNull @PositiveOrZero Integer> previewSequence) {
	}
}
