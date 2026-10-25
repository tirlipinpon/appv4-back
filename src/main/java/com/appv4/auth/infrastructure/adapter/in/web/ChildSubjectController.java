package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ChildSubjectUseCase;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.result.ChildSubjectsView;
import com.appv4.auth.application.result.ChildSubjectsView.SubjectOffer;
import com.appv4.auth.application.result.ParentSubjectsView;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Matières des enfants du parent (SCRUM-162).
 *
 * <p><b>Business :</b> la page montre chaque élève. Les matières de son école et
 * de son année sont déjà dans sa liste. Il ajoute les autres, ou en retire.
 * <p><b>Technique :</b> {@code /api/int/parent/subjects}, JWT, espace parent.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link ChildSubjectUseCase}.
 */
@RestController
@RequestMapping("/api/int/parent/subjects")
@Tag(name = "Parent — matières des enfants")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class ChildSubjectController {

	private final ChildSubjectUseCase childSubjectUseCase;

	public ChildSubjectController(ChildSubjectUseCase childSubjectUseCase) {
		this.childSubjectUseCase = childSubjectUseCase;
	}

	@GetMapping
	@Operation(operationId = "listParentSubjects", summary = "Lister les enfants et leurs matières")
	@ApiResponse(responseCode = "200", description = "Chaque enfant, avec les matières déjà choisies et celles proposées")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ParentSubjectsResponse> list(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(toResponse(childSubjectUseCase.loadAll(user.userId(), user.activeRole())));
	}

	@PostMapping
	@Operation(operationId = "addParentChildSubject", summary = "Ajouter une matière à un enfant")
	@ApiResponse(responseCode = "200", description = "Listes après l'ajout")
	@ApiResponse(
			responseCode = "400",
			description = "Matière introuvable, plus visible, ou déjà dans la liste",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Enfant introuvable pour ce parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ParentSubjectsResponse> add(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestBody AddChildSubjectRequest request) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		AddChildSubjectRequest body = request == null ? new AddChildSubjectRequest(null, null) : request;
		ParentSubjectsView view = childSubjectUseCase.add(
				user.userId(), user.activeRole(), body.childId() == null ? -1L : body.childId(), body.subjectId());
		return ResponseEntity.ok(toResponse(view));
	}

	@DeleteMapping("/{childId}/{subjectId}")
	@Operation(operationId = "removeParentChildSubject", summary = "Retirer une matière de la liste d'un enfant")
	@ApiResponse(responseCode = "200", description = "Listes après le retrait. La matière du professeur reste")
	@ApiResponse(
			responseCode = "404",
			description = "Enfant introuvable, ou matière absente de sa liste",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ParentSubjectsResponse> remove(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long childId,
			@PathVariable long subjectId) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(toResponse(
				childSubjectUseCase.remove(user.userId(), user.activeRole(), childId, subjectId)));
	}

	private static ParentSubjectsResponse toResponse(ParentSubjectsView view) {
		return new ParentSubjectsResponse(view.children().stream().map(ChildSubjectController::child).toList());
	}

	private static ChildSubjectsResponse child(ChildSubjectsView view) {
		return new ChildSubjectsResponse(
				view.childId(),
				view.firstName(),
				view.lastName(),
				view.schoolId(),
				view.schoolName(),
				view.grade(),
				view.chosen().stream().map(ChildSubjectController::offer).toList(),
				view.proposed().stream().map(ChildSubjectController::offer).toList());
	}

	private static ChildSubjectOffer offer(SubjectOffer offer) {
		return new ChildSubjectOffer(
				offer.subjectId(),
				offer.name(),
				offer.teacherName(),
				offer.schoolId(),
				offer.schoolName(),
				offer.grade(),
				offer.scolaire());
	}

	@Schema(name = "ParentSubjectsResponse")
	public record ParentSubjectsResponse(List<ChildSubjectsResponse> children) {
	}

	@Schema(name = "ChildSubjectsResponse")
	public record ChildSubjectsResponse(
			long childId,
			String firstName,
			String lastName,
			Long schoolId,
			String schoolName,
			String grade,
			List<ChildSubjectOffer> chosen,
			List<ChildSubjectOffer> proposed) {
	}

	@Schema(name = "ChildSubjectOffer")
	public record ChildSubjectOffer(
			long subjectId,
			String name,
			String teacherName,
			long schoolId,
			String schoolName,
			String grade,
			boolean scolaire) {
	}

	@Schema(name = "AddChildSubjectRequest")
	public record AddChildSubjectRequest(Long childId, Long subjectId) {
	}
}
