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

import com.appv4.auth.application.port.in.HumeurLibraryUseCase;
import com.appv4.auth.application.port.out.HumeurChoicePort.HumeurChoice;
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
 * Bibliothèque des humeurs (BACK-68).
 *
 * <p><b>Business :</b> l'admin compose les choix. Un parent ou un enfant n'écrit pas ici.
 * <p><b>Technique :</b> {@code /api/int/admin/humeurs}, JWT. L'icône ou le mot absent
 * n'est pas renvoyé.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link HumeurLibraryUseCase}.
 */
@RestController
@RequestMapping("/api/int/admin/humeurs")
@Tag(name = "Admin — humeurs")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class HumeurLibraryController {

	private final HumeurLibraryUseCase humeurLibraryUseCase;

	public HumeurLibraryController(HumeurLibraryUseCase humeurLibraryUseCase) {
		this.humeurLibraryUseCase = humeurLibraryUseCase;
	}

	@GetMapping
	@Operation(operationId = "listHumeurChoices", summary = "Lire la bibliothèque des humeurs")
	@ApiResponse(responseCode = "200", description = "Choix dans l'ordre de création")
	@ApiResponse(
			responseCode = "403",
			description = "Le compte n'est pas admin",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<HumeurChoiceListResponse> list(@AuthenticationPrincipal AuthenticatedUser principal) {
		List<HumeurChoiceItem> choices = humeurLibraryUseCase.list(CurrentAuth.require(principal).userId()).stream()
				.map(HumeurLibraryController::toItem)
				.toList();
		return ResponseEntity.ok(new HumeurChoiceListResponse(choices));
	}

	@PostMapping
	@Operation(operationId = "createHumeurChoice", summary = "Ajouter un choix d'humeur")
	@ApiResponse(responseCode = "200", description = "Choix enregistré")
	@ApiResponse(
			responseCode = "400",
			description = "Ni icône ni mot",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le compte n'est pas admin",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<HumeurChoiceItem> create(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestBody HumeurChoiceRequest request) {
		HumeurChoice saved = humeurLibraryUseCase.create(
				CurrentAuth.require(principal).userId(), request.icon(), request.word());
		return ResponseEntity.ok(toItem(saved));
	}

	@PutMapping("/{id}")
	@Operation(operationId = "updateHumeurChoice", summary = "Modifier un choix d'humeur")
	@ApiResponse(responseCode = "200", description = "Choix modifié, même identifiant")
	@ApiResponse(
			responseCode = "400",
			description = "Ni icône ni mot",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le compte n'est pas admin",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Choix inconnu",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<HumeurChoiceItem> update(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id,
			@RequestBody HumeurChoiceRequest request) {
		HumeurChoice saved = humeurLibraryUseCase.update(
				CurrentAuth.require(principal).userId(), id, request.icon(), request.word());
		return ResponseEntity.ok(toItem(saved));
	}

	@DeleteMapping("/{id}")
	@Operation(operationId = "deleteHumeurChoice", summary = "Retirer un choix d'humeur")
	@ApiResponse(responseCode = "204", description = "Choix retiré")
	@ApiResponse(
			responseCode = "403",
			description = "Le compte n'est pas admin",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Choix inconnu",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id) {
		humeurLibraryUseCase.delete(CurrentAuth.require(principal).userId(), id);
		return ResponseEntity.noContent().build();
	}

	private static HumeurChoiceItem toItem(HumeurChoice choice) {
		return new HumeurChoiceItem(choice.id(), choice.icon(), choice.word());
	}

	@Schema(name = "HumeurChoiceRequest")
	public record HumeurChoiceRequest(String icon, String word) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@Schema(name = "HumeurChoiceItem")
	public record HumeurChoiceItem(long id, String icon, String word) {
	}

	@Schema(name = "HumeurChoiceListResponse")
	public record HumeurChoiceListResponse(List<HumeurChoiceItem> choices) {
	}
}
