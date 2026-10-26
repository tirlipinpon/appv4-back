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
import com.appv4.auth.application.result.TeacherSubjectColleaguesView;
import com.appv4.auth.application.result.TeacherSubjectsView;
import com.appv4.auth.application.result.TeacherSubjectsView.SubjectLine;
import com.appv4.auth.application.result.TeacherTransferInvitesView;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Matières du professeur connecté (SCRUM-161, SCRUM-172, SCRUM-189).
 *
 * <p><b>Business :</b> il crée, voit, renomme, retire, désactive et propose un
 * transfert (déplacer ou partager). Le collègue accepte ou refuse. Pas d'exercice.
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
		return ResponseEntity.ok(item(line));
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
		return ResponseEntity.ok(item(line));
	}

	@DeleteMapping("/{id}")
	@Operation(operationId = "deleteTeacherSubject", summary = "Retirer une matière du professeur connecté")
	@ApiResponse(responseCode = "204", description = "Accès retiré. Cachée seulement s'il ne reste personne")
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
	@ApiResponse(responseCode = "204", description = "Reste dans les listes, plus proposée dans le reste de l'application")
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

	@GetMapping("/{id}/colleagues")
	@Operation(
			operationId = "listTeacherSubjectColleagues",
			summary = "Lister les collègues à qui proposer un transfert")
	@ApiResponse(responseCode = "200", description = "Professeurs acceptés avec la même classe, triés par nom")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière introuvable pour ce professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherSubjectColleaguesResponse> colleagues(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		TeacherSubjectColleaguesView view =
				teacherSubjectUseCase.listColleagues(user.userId(), user.activeRole(), id);
		List<TeacherSubjectColleagueItem> items = view.colleagues().stream()
				.map(line -> new TeacherSubjectColleagueItem(line.userId(), line.displayName()))
				.toList();
		return ResponseEntity.ok(new TeacherSubjectColleaguesResponse(items));
	}

	@PostMapping("/{id}/transfer")
	@Operation(
			operationId = "transferTeacherSubject",
			summary = "Proposer de déplacer ou partager une matière avec un collègue")
	@ApiResponse(responseCode = "204", description = "Demande créée ; l'accès ne change qu'après acceptation")
	@ApiResponse(
			responseCode = "400",
			description = "Mode, collègue ou demande déjà en attente",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Matière introuvable pour ce professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<Void> transfer(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id,
			@RequestBody TransferTeacherSubjectRequest request) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		TransferTeacherSubjectRequest body = request == null
				? new TransferTeacherSubjectRequest(null, null)
				: request;
		teacherSubjectUseCase.requestTransfer(
				user.userId(), user.activeRole(), id, body.colleagueUserId(), body.mode());
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/transfer-invites")
	@Operation(
			operationId = "listTeacherSubjectTransferInvites",
			summary = "Lister les demandes de transfert reçues")
	@ApiResponse(responseCode = "200", description = "Demandes en attente avec le détail matière et mode")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherTransferInvitesResponse> transferInvites(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		TeacherTransferInvitesView view =
				teacherSubjectUseCase.listTransferInvites(user.userId(), user.activeRole());
		List<TeacherTransferInviteItem> items = view.invites().stream()
				.map(line -> new TeacherTransferInviteItem(
						line.id(),
						line.subjectId(),
						line.subjectName(),
						line.schoolId(),
						line.schoolName(),
						line.grade(),
						line.mode(),
						line.fromDisplayName()))
				.toList();
		return ResponseEntity.ok(new TeacherTransferInvitesResponse(items));
	}

	@PostMapping("/transfer-invites/{inviteId}/accept")
	@Operation(operationId = "acceptTeacherSubjectTransfer", summary = "Accepter une demande de transfert")
	@ApiResponse(responseCode = "204", description = "Accès mis à jour (même matière, pas une copie)")
	@ApiResponse(
			responseCode = "400",
			description = "Demande plus valable",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Demande introuvable",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<Void> acceptTransfer(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long inviteId) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		teacherSubjectUseCase.acceptTransfer(user.userId(), user.activeRole(), inviteId);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/transfer-invites/{inviteId}/reject")
	@Operation(operationId = "rejectTeacherSubjectTransfer", summary = "Refuser une demande de transfert")
	@ApiResponse(responseCode = "204", description = "Demande refusée, accès inchangé")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Demande introuvable",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<Void> rejectTransfer(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long inviteId) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		teacherSubjectUseCase.rejectTransfer(user.userId(), user.activeRole(), inviteId);
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
		return new TeacherSubjectItem(
				subject.id(), subject.name(), subject.active(), subject.sharedWith());
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
	public record TeacherSubjectItem(long id, String name, boolean active, List<String> sharedWith) {
	}

	@Schema(name = "CreateTeacherSubjectRequest")
	public record CreateTeacherSubjectRequest(Long schoolId, String grade, String name) {
	}

	@Schema(name = "RenameTeacherSubjectRequest")
	public record RenameTeacherSubjectRequest(String name) {
	}

	@Schema(name = "TransferTeacherSubjectRequest")
	public record TransferTeacherSubjectRequest(
			@Schema(description = "Identifiant du collègue choisi dans la liste") Long colleagueUserId,
			@Schema(description = "move ou share", allowableValues = { "move", "share" }) String mode) {
	}

	@Schema(name = "TeacherSubjectColleaguesResponse")
	public record TeacherSubjectColleaguesResponse(List<TeacherSubjectColleagueItem> colleagues) {
	}

	@Schema(name = "TeacherSubjectColleagueItem")
	public record TeacherSubjectColleagueItem(long userId, String displayName) {
	}

	@Schema(name = "TeacherTransferInvitesResponse")
	public record TeacherTransferInvitesResponse(List<TeacherTransferInviteItem> invites) {
	}

	@Schema(name = "TeacherTransferInviteItem")
	public record TeacherTransferInviteItem(
			long id,
			long subjectId,
			String subjectName,
			long schoolId,
			String schoolName,
			String grade,
			String mode,
			String fromDisplayName) {
	}
}
