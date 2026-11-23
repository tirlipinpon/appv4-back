package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.SejourUseCase;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.infrastructure.adapter.in.web.ParentSejourController.ParentSejourItem;
import com.appv4.auth.infrastructure.adapter.in.web.ParentSejourController.ParentSejourListResponse;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Lecture professeur des séjours d'un élève (BACK-82).
 *
 * <p><b>Business :</b> le professeur ouvre un élève de sa classe et voit le même
 * détail que le parent : Accueil, les autres matières, la date et l'heure.
 * Un élève hors classe est refusé avec la phrase déjà connue.
 * <p><b>Technique :</b> {@code GET /api/int/teacher/students/{childId}/sejours}.
 * Le corps est celui de la lecture parent. Rien n'est recompté ici.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link SejourUseCase#readTeacher},
 * qui contrôle la classe puis lit la liste partagée.
 */
@RestController
@RequestMapping("/api/int/teacher/students/{childId}/sejours")
@Tag(name = "Professeur — séjours")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class TeacherSejourController {

	private final SejourUseCase sejourUseCase;

	public TeacherSejourController(SejourUseCase sejourUseCase) {
		this.sejourUseCase = sejourUseCase;
	}

	@GetMapping
	@Operation(operationId = "listTeacherSejours", summary = "Lire les séjours d'un élève de la classe")
	@ApiResponse(responseCode = "200", description = "Même détail que la lecture parent, ou liste vide")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace professeur",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Cet élève n'est pas dans tes classes",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ParentSejourListResponse> list(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long childId) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		List<ParentSejourItem> sejours = sejourUseCase.readTeacher(user.userId(), user.activeRole(), childId)
				.stream()
				.map(ParentSejourController::toItem)
				.toList();
		return ResponseEntity.ok(new ParentSejourListResponse(sejours));
	}
}
