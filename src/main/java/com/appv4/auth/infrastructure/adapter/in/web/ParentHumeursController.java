package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ReadParentHumeursUseCase;
import com.appv4.auth.application.port.in.ReadParentHumeursUseCase.Humeur;
import com.appv4.auth.application.port.in.ReadParentHumeursUseCase.ParentChildHumeur;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.configuration.OpenApiConfiguration;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Dernière humeur des enfants du parent (BACK-71).
 *
 * <p><b>Business :</b> la page Humeur lit cette route. Le suivi ne la reçoit pas.
 * <p><b>Technique :</b> {@code GET /api/int/parent/humeurs}. Un champ absent n'est pas renvoyé.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link ReadParentHumeursUseCase}.
 */
@RestController
@RequestMapping("/api/int/parent/humeurs")
@Tag(name = "Parent — humeurs")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class ParentHumeursController {

	private final ReadParentHumeursUseCase readParentHumeursUseCase;

	public ParentHumeursController(ReadParentHumeursUseCase readParentHumeursUseCase) {
		this.readParentHumeursUseCase = readParentHumeursUseCase;
	}

	@GetMapping
	@Operation(operationId = "listParentHumeurs", summary = "Lire les humeurs de chaque enfant")
	@ApiResponse(responseCode = "200", description = "Enfants du parent, avec leur historique d'humeurs")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ParentHumeurListResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		List<ParentHumeurChildItem> children = readParentHumeursUseCase.list(user.userId(), user.activeRole()).stream()
				.map(ParentHumeursController::toItem)
				.toList();
		return ResponseEntity.ok(new ParentHumeurListResponse(children));
	}

	private static ParentHumeurChildItem toItem(ParentChildHumeur child) {
		List<ParentHumeurItem> humeurs = child.humeurs().stream().map(ParentHumeursController::toHumeur).toList();
		return new ParentHumeurChildItem(child.id(), child.firstName(), child.lastName(), humeurs);
	}

	private static ParentHumeurItem toHumeur(Humeur humeur) {
		return new ParentHumeurItem(humeur.icon(), humeur.word(), humeur.label(), humeur.dateLabel(), humeur.timeLabel());
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(name = "ParentHumeurItem")
	public record ParentHumeurItem(String icon, String word, String label, String dateLabel, String timeLabel) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(name = "ParentHumeurChildItem")
	public record ParentHumeurChildItem(long id, String firstName, String lastName, List<ParentHumeurItem> humeurs) {
	}

	@Schema(name = "ParentHumeurListResponse")
	public record ParentHumeurListResponse(List<ParentHumeurChildItem> children) {
	}
}
