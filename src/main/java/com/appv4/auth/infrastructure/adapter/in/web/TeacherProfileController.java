package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.TeacherProfileUseCase;
import com.appv4.auth.application.port.in.TeacherProfileUseCase.TeacherSchoolCommand;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.result.TeacherProfileView;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Profil du professeur connecté (SCRUM-159).
 *
 * <p><b>Business :</b> il voit son email, corrige son nom, et ne rattache une école
 * que si le référentiel en propose. Un parent ou une demande en attente n'ouvre pas ça.
 * <p><b>Technique :</b> {@code GET} et {@code PUT /api/int/teacher/profile}, JWT obligatoire.
 * L'espace actif du jeton doit être {@code teacher}.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link TeacherProfileUseCase}.
 */
@RestController
@RequestMapping("/api/int/teacher/profile")
@Tag(name = "Professeur — profil")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class TeacherProfileController {

	private final TeacherProfileUseCase teacherProfileUseCase;

	public TeacherProfileController(TeacherProfileUseCase teacherProfileUseCase) {
		this.teacherProfileUseCase = teacherProfileUseCase;
	}

	@GetMapping
	@Operation(operationId = "getTeacherProfile", summary = "Lire le profil du professeur connecté")
	@ApiResponse(responseCode = "200", description = "Email, nom, écoles déjà choisies et écoles proposables")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherProfileResponse> getProfile(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(toResponse(teacherProfileUseCase.load(user.userId(), user.activeRole())));
	}

	@PutMapping
	@Operation(operationId = "updateTeacherProfile", summary = "Enregistrer le nom et les écoles du professeur")
	@ApiResponse(responseCode = "200", description = "Profil enregistré")
	@ApiResponse(
			responseCode = "400",
			description = "Nom vide ou trop long, école inconnue, école en double, ou année manquante",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherProfileResponse> updateProfile(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestBody UpdateTeacherProfileRequest request) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		UpdateTeacherProfileRequest body = request == null
				? new UpdateTeacherProfileRequest(null, List.of())
				: request;
		TeacherProfileView view = teacherProfileUseCase.update(
				user.userId(),
				user.activeRole(),
				body.displayName(),
				toCommands(body.schools()));
		return ResponseEntity.ok(toResponse(view));
	}

	private static List<TeacherSchoolCommand> toCommands(List<TeacherSchoolInput> schools) {
		if (schools == null || schools.isEmpty()) {
			return List.of();
		}
		return schools.stream()
				.map(school -> new TeacherSchoolCommand(school.schoolId(), school.curriculum(), school.grades()))
				.toList();
	}

	private static TeacherProfileResponse toResponse(TeacherProfileView view) {
		List<TeacherAssignedSchool> schools = view.schools().stream()
				.map(school -> new TeacherAssignedSchool(
						school.schoolId(), school.schoolName(), school.curriculum(), school.grades()))
				.toList();
		List<TeacherAvailableSchool> available = view.availableSchools().stream()
				.map(school -> new TeacherAvailableSchool(school.id(), school.name()))
				.toList();
		List<TeacherCurriculumOption> curricula = view.curricula().stream()
				.map(choice -> new TeacherCurriculumOption(choice.code(), choice.label(), choice.grades()))
				.toList();
		return new TeacherProfileResponse(view.email(), view.displayName(), schools, available, curricula);
	}

	@Schema(name = "TeacherProfileResponse")
	public record TeacherProfileResponse(
			String email,
			String displayName,
			List<TeacherAssignedSchool> schools,
			List<TeacherAvailableSchool> availableSchools,
			List<TeacherCurriculumOption> curricula) {
	}

	@Schema(name = "TeacherAssignedSchool")
	public record TeacherAssignedSchool(
			long schoolId,
			String schoolName,
			String curriculum,
			List<String> grades) {
	}

	@Schema(name = "TeacherAvailableSchool")
	public record TeacherAvailableSchool(long id, String name) {
	}

	@Schema(name = "TeacherCurriculumOption")
	public record TeacherCurriculumOption(String code, String label, List<String> grades) {
	}

	@Schema(name = "UpdateTeacherProfileRequest")
	public record UpdateTeacherProfileRequest(String displayName, List<TeacherSchoolInput> schools) {
	}

	@Schema(name = "TeacherSchoolInput")
	public record TeacherSchoolInput(Long schoolId, String curriculum, List<String> grades) {
	}
}
