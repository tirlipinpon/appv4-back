package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.TeacherSubjectUseCase;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.result.TeacherSubjectsView;
import com.appv4.auth.application.result.TeacherSubjectsView.SubjectLine;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Matières du professeur connecté (SCRUM-161).
 *
 * <p><b>Business :</b> il crée, voit, renomme et retire un nom dans chacune de ses
 * classes. Un parent, ou une demande professeur encore en attente, n'ouvre pas ça.
 * Pas d'exercice, pas de forme de question, pas de copie vers un autre professeur.
 * <p><b>Technique :</b> {@code /api/int/teacher/subjects}, JWT obligatoire.
 * L'espace actif du jeton doit être {@code teacher}.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link TeacherSubjectUseCase}.
 */
@RestController
@RequestMapping("/api/int/teacher/subjects")
@Tag(name = "Professeur — matières")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class TeacherSubjectController {

	private final TeacherSubjectUseCase teacherSubjectUseCase;

	public TeacherSubjectController(TeacherSubjectUseCase teacherSubjectUseCase) {
		this.teacherSubjectUseCase = teacherSubjectUseCase;
	}

	@GetMapping
	@Operation(operationId = "listTeacherSubjects", summary = "Lister les matières du professeur connecté")
	@ApiResponse(responseCode = "200", description = "Écoles du profil, années cochées, matières triées par nom")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherSubjectsResponse> list(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(toResponse(teacherSubjectUseCase.list(user.userId(), user.activeRole())));
	}

	@PostMapping
	@Operation(operationId = "createTeacherSubject", summary = "Créer une matière dans une classe du professeur")
	@ApiResponse(responseCode = "200", description = "Matière créée")
	@ApiResponse(
			responseCode = "400",
			description = "Nom vide, trop long, déjà pris dans la classe, ou classe absente du profil",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherSubjectItem> create(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestBody CreateTeacherSubjectRequest request) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		CreateTeacherSubjectRequest body = request == null
				? new CreateTeacherSubjectRequest(null, null, null)
				: request;
		SubjectLine line = teacherSubjectUseCase.create(
				user.userId(), user.activeRole(), body.schoolId(), body.grade(), body.name());
		return ResponseEntity.ok(new TeacherSubjectItem(line.id(), line.name(), line.active()));
	}

	@PutMapping("/{id}")
	@Operation(operationId = "renameTeacherSubject", summary = "Renommer une matière du professeur connecté")
	@ApiResponse(responseCode = "200", description = "Nom enregistré, la classe ne change pas")
	@ApiResponse(
			responseCode = "400",
			description = "Nom vide, trop long, ou déjà pris dans la même classe",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière introuvable pour ce professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherSubjectItem> rename(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id,
			@RequestBody RenameTeacherSubjectRequest request) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		RenameTeacherSubjectRequest body = request == null ? new RenameTeacherSubjectRequest(null) : request;
		SubjectLine line = teacherSubjectUseCase.rename(user.userId(), user.activeRole(), id, body.name());
		return ResponseEntity.ok(new TeacherSubjectItem(line.id(), line.name(), line.active()));
	}

	@DeleteMapping("/{id}")
	@Operation(operationId = "deleteTeacherSubject", summary = "Retirer une matière du professeur connecté")
	@ApiResponse(responseCode = "204", description = "Retirée de la page. La ligne reste en base")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière introuvable pour ce professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		teacherSubjectUseCase.remove(user.userId(), user.activeRole(), id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/deactivate")
	@Operation(operationId = "deactivateTeacherSubject", summary = "Désactiver une matière du professeur connecté")
	@ApiResponse(responseCode = "204", description = "Reste dans sa liste, plus proposée dans le reste de l'application")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière introuvable pour ce professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<Void> deactivate(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		teacherSubjectUseCase.deactivate(user.userId(), user.activeRole(), id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/reactivate")
	@Operation(operationId = "reactivateTeacherSubject", summary = "Réactiver une matière du professeur connecté")
	@ApiResponse(responseCode = "204", description = "De nouveau proposée dans le reste de l'application")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière introuvable pour ce professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<Void> reactivate(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		teacherSubjectUseCase.reactivate(user.userId(), user.activeRole(), id);
		return ResponseEntity.noContent().build();
	}

	private static TeacherSubjectsResponse toResponse(TeacherSubjectsView view) {
		List<TeacherSubjectSchool> schools = view.schools().stream()
				.map(school -> new TeacherSubjectSchool(
						school.schoolId(),
						school.schoolName(),
						school.grades().stream()
								.map(grade -> new TeacherSubjectGrade(
										grade.grade(),
										grade.subjects().stream()
												.map(TeacherSubjectController::item)
												.toList()))
								.toList()))
				.toList();
		return new TeacherSubjectsResponse(schools);
	}

	private static TeacherSubjectItem item(SubjectLine subject) {
		return new TeacherSubjectItem(subject.id(), subject.name(), subject.active());
	}

	@Schema(name = "TeacherSubjectsResponse")
	public record TeacherSubjectsResponse(List<TeacherSubjectSchool> schools) {
	}

	@Schema(name = "TeacherSubjectSchool")
	public record TeacherSubjectSchool(long schoolId, String schoolName, List<TeacherSubjectGrade> grades) {
	}

	@Schema(name = "TeacherSubjectGrade")
	public record TeacherSubjectGrade(String grade, List<TeacherSubjectItem> subjects) {
	}

	@Schema(name = "TeacherSubjectItem")
	public record TeacherSubjectItem(long id, String name, boolean active) {
	}

	@Schema(name = "CreateTeacherSubjectRequest")
	public record CreateTeacherSubjectRequest(Long schoolId, String grade, String name) {
	}

	@Schema(name = "RenameTeacherSubjectRequest")
	public record RenameTeacherSubjectRequest(String name) {
	}
}
