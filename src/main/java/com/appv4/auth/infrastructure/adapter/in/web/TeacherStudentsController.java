package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.TeacherStudentsUseCase;
import com.appv4.auth.application.port.in.TeacherStudentsUseCase.StudentPhoto;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.result.TeacherStudentsView;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Élèves des classes du professeur connecté (SCRUM-160).
 *
 * <p><b>Business :</b> il voit les enfants inscrits dans une école et une année de son profil.
 * La page est en lecture seule : pas de création, pas de modification, pas de PIN, pas de parent.
 * <p><b>Technique :</b> {@code /api/int/teacher/students}, JWT obligatoire.
 * L'espace actif du jeton doit être {@code teacher}.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link TeacherStudentsUseCase}.
 */
@RestController
@RequestMapping("/api/int/teacher/students")
@Tag(name = "Professeur — élèves")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class TeacherStudentsController {

	private final TeacherStudentsUseCase teacherStudentsUseCase;

	public TeacherStudentsController(TeacherStudentsUseCase teacherStudentsUseCase) {
		this.teacherStudentsUseCase = teacherStudentsUseCase;
	}

	@GetMapping
	@Operation(operationId = "listTeacherStudents", summary = "Lister les élèves des classes du professeur")
	@ApiResponse(responseCode = "200", description = "Écoles du profil, années cochées, élèves triés par nom")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<TeacherStudentsResponse> list(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(toResponse(teacherStudentsUseCase.list(user.userId(), user.activeRole())));
	}

	@GetMapping("/{id}/photo")
	@Operation(operationId = "getTeacherStudentPhoto", summary = "Lire la photo d'un élève d'une classe du professeur")
	@ApiResponse(responseCode = "200", description = "Image de l'élève")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Élève hors des classes du professeur, ou fiche sans photo",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<byte[]> photo(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		StudentPhoto photo = teacherStudentsUseCase.photo(user.userId(), user.activeRole(), id);
		String type = photo.contentType() == null || photo.contentType().isBlank()
				? MediaType.IMAGE_JPEG_VALUE
				: photo.contentType();
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(type))
				.body(photo.bytes());
	}

	private static TeacherStudentsResponse toResponse(TeacherStudentsView view) {
		List<TeacherStudentSchool> schools = view.schools().stream()
				.map(school -> new TeacherStudentSchool(
						school.schoolId(),
						school.schoolName(),
						school.curriculumLabel(),
						school.grades().stream()
								.map(grade -> new TeacherStudentGrade(
										grade.grade(),
										grade.countLabel(),
										grade.emptyMessage(),
										grade.students().stream()
												.map(TeacherStudentsController::line)
												.toList()))
								.toList()))
				.toList();
		return new TeacherStudentsResponse(view.noClassMessage(), schools);
	}

	private static TeacherStudentLine line(TeacherStudentsView.StudentLine student) {
		return new TeacherStudentLine(
				student.id(),
				student.firstName(),
				student.lastName(),
				student.age(),
				student.avatarCode(),
				student.hasPhoto());
	}

	@Schema(name = "TeacherStudentsResponse")
	public record TeacherStudentsResponse(String noClassMessage, List<TeacherStudentSchool> schools) {
	}

	@Schema(name = "TeacherStudentSchool")
	public record TeacherStudentSchool(
			long schoolId,
			String schoolName,
			String curriculumLabel,
			List<TeacherStudentGrade> grades) {
	}

	@Schema(name = "TeacherStudentGrade")
	public record TeacherStudentGrade(
			String grade,
			String countLabel,
			String emptyMessage,
			List<TeacherStudentLine> students) {
	}

	@Schema(name = "TeacherStudentLine")
	public record TeacherStudentLine(
			long id,
			String firstName,
			String lastName,
			int age,
			String avatarCode,
			boolean hasPhoto) {
	}
}
