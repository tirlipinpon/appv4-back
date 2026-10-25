package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.MediaType;
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

import com.appv4.auth.application.port.in.ParentChildrenUseCase;
import com.appv4.auth.application.port.in.ParentChildrenUseCase.NewChildCommand;
import com.appv4.auth.application.port.out.ChildPort.ChildPhoto;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.result.ParentChildrenView;
import com.appv4.auth.application.result.ParentChildrenView.ChildCard;
import com.appv4.configuration.OpenApiConfiguration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Enfants du parent connecté (SCRUM-156).
 *
 * <p><b>Business :</b> il ajoute un enfant, puis un autre, et les retrouve ici.
 * Le code PIN n'est pas renvoyé. Un professeur n'ouvre pas cette page.
 * <p><b>Technique :</b> {@code GET} et {@code POST /api/int/parent/children}, JWT, espace parent.
 * <p><b>Métier / flow :</b> filtre JWT → ce controller → {@link ParentChildrenUseCase}.
 */
@RestController
@RequestMapping("/api/int/parent/children")
@Tag(name = "Parent — enfants")
@SecurityRequirement(name = OpenApiConfiguration.BEARER_JWT)
public class ParentChildrenController {

	private final ParentChildrenUseCase parentChildrenUseCase;

	public ParentChildrenController(ParentChildrenUseCase parentChildrenUseCase) {
		this.parentChildrenUseCase = parentChildrenUseCase;
	}

	@GetMapping
	@Operation(operationId = "listParentChildren", summary = "Lister les enfants du parent connecté")
	@ApiResponse(responseCode = "200", description = "Fiches du parent et écoles proposables")
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ParentChildrenResponse> list(
			@AuthenticationPrincipal AuthenticatedUser principal) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		return ResponseEntity.ok(toResponse(parentChildrenUseCase.list(user.userId(), user.activeRole())));
	}

	@PostMapping
	@Operation(operationId = "addParentChild", summary = "Ajouter un enfant au parent connecté")
	@ApiResponse(responseCode = "200", description = "Fiche créée, sans le PIN")
	@ApiResponse(
			responseCode = "400",
			description = "Champ obligatoire manquant, âge, classe, image, PIN ou lien refusé",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "403",
			description = "Le jeton n'est pas dans l'espace parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "L'enfant de départ n'appartient pas à ce parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ParentChildResponse> add(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@RequestBody CreateParentChildRequest request) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		CreateParentChildRequest body = request == null
				? new CreateParentChildRequest(
						null, null, null, null, null, null, null, null, null, null, null, null, null, null)
				: request;
		ChildCard card = parentChildrenUseCase.add(
				user.userId(),
				user.activeRole(),
				new NewChildCommand(
						body.firstName(),
						body.lastName(),
						body.sex(),
						body.age(),
						body.country(),
						body.grade(),
						body.schoolId(),
						body.avatarCode(),
						body.photoBase64(),
						body.photoContentType(),
						body.pin(),
						body.pinConfirmation(),
						body.fromChildId(),
						body.link()));
		return ResponseEntity.ok(toChild(card));
	}

	private static NewChildCommand toCommand(CreateParentChildRequest request) {
		CreateParentChildRequest body = request == null
				? new CreateParentChildRequest(
						null, null, null, null, null, null, null, null, null, null, null, null, null, null)
				: request;
		return new NewChildCommand(
				body.firstName(),
				body.lastName(),
				body.sex(),
				body.age(),
				body.country(),
				body.grade(),
				body.schoolId(),
				body.avatarCode(),
				body.photoBase64(),
				body.photoContentType(),
				body.pin(),
				body.pinConfirmation(),
				body.fromChildId(),
				body.link());
	}

	@PutMapping("/{id}")
	@Operation(operationId = "updateParentChild", summary = "Modifier un enfant du parent connecté")
	@ApiResponse(responseCode = "200", description = "Fiche mise à jour, sans le PIN")
	@ApiResponse(
			responseCode = "400",
			description = "Champ refusé",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Enfant introuvable pour ce parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ParentChildResponse> update(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id,
			@RequestBody CreateParentChildRequest request) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		ChildCard card = parentChildrenUseCase.update(user.userId(), user.activeRole(), id, toCommand(request));
		return ResponseEntity.ok(toChild(card));
	}

	@PostMapping("/{id}/deactivate")
	@Operation(operationId = "deactivateParentChild", summary = "Désactiver un enfant du parent connecté")
	@ApiResponse(responseCode = "204", description = "Toujours affiché dans la liste, plus proposé ailleurs")
	@ApiResponse(
			responseCode = "404",
			description = "Enfant introuvable pour ce parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<Void> deactivate(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		parentChildrenUseCase.deactivate(user.userId(), user.activeRole(), id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/reactivate")
	@Operation(operationId = "reactivateParentChild", summary = "Réactiver un enfant du parent connecté")
	@ApiResponse(responseCode = "204", description = "De nouveau proposé dans le reste de l'application")
	@ApiResponse(
			responseCode = "404",
			description = "Enfant introuvable pour ce parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<Void> reactivate(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		parentChildrenUseCase.reactivate(user.userId(), user.activeRole(), id);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}")
	@Operation(operationId = "deleteParentChild", summary = "Retirer un enfant de l'affichage")
	@ApiResponse(responseCode = "204", description = "Retiré de l'affichage, fiche conservée en base")
	@ApiResponse(
			responseCode = "404",
			description = "Enfant introuvable pour ce parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		parentChildrenUseCase.hide(user.userId(), user.activeRole(), id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{id}/photo")
	@Operation(operationId = "getParentChildPhoto", summary = "Lire la photo d'un enfant du parent connecté")
	@ApiResponse(responseCode = "200", description = "Image de l'enfant")
	@ApiResponse(
			responseCode = "404",
			description = "Pas de photo pour cet enfant, ou enfant d'un autre parent",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<byte[]> photo(
			@AuthenticationPrincipal AuthenticatedUser principal,
			@PathVariable long id) {
		AuthenticatedUser user = CurrentAuth.require(principal);
		ChildPhoto photo = parentChildrenUseCase.photo(user.userId(), user.activeRole(), id);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(photo.contentType()))
				.body(photo.bytes());
	}

	private static ParentChildrenResponse toResponse(ParentChildrenView view) {
		List<ParentChildResponse> children = view.children().stream()
				.map(ParentChildrenController::toChild)
				.toList();
		List<ParentSchoolOption> schools = view.schools().stream()
				.map(school -> new ParentSchoolOption(school.id(), school.name(), school.country(), school.grades()))
				.toList();
		List<ParentGradeList> curricula = view.curricula().stream()
				.map(list -> new ParentGradeList(list.country(), list.grades()))
				.toList();
		return new ParentChildrenResponse(children, schools, curricula);
	}

	private static ParentChildResponse toChild(ChildCard card) {
		List<ParentChildLink> links = card.links().stream()
				.map(mark -> new ParentChildLink(mark.otherChildId(), mark.label()))
				.toList();
		return new ParentChildResponse(
				card.id(),
				card.firstName(),
				card.lastName(),
				card.sex(),
				card.age(),
				card.country(),
				card.curriculum(),
				card.grade(),
				card.schoolId(),
				card.schoolName(),
				card.avatarCode(),
				card.hasPhoto(),
				card.active(),
				card.linkPhrases(),
				links);
	}

	@Schema(name = "ParentChildrenResponse")
	public record ParentChildrenResponse(
			List<ParentChildResponse> children,
			List<ParentSchoolOption> schools,
			List<ParentGradeList> curricula) {
	}

	@Schema(name = "ParentGradeList")
	public record ParentGradeList(String country, List<String> grades) {
	}

	@Schema(name = "ParentChildResponse")
	public record ParentChildResponse(
			long id,
			String firstName,
			String lastName,
			@Schema(description = "GARCON, FILLE ou NON_GENRE")
			String sex,
			int age,
			String country,
			String curriculum,
			String grade,
			Long schoolId,
			String schoolName,
			String avatarCode,
			boolean hasPhoto,
			@Schema(description = "Faux : encore dans la liste, plus proposé dans le reste de l'application")
			boolean active,
			@Schema(description = "Phrases de lien, par exemple « Noah est le frère de Léa ». Vide s'il n'y en a pas.")
			List<String> linkPhrases,
			@Schema(description = "L'autre enfant du lien, pour afficher sa photo ou son avatar, et le mot court.")
			List<ParentChildLink> links) {
	}

	@Schema(name = "ParentChildLink")
	public record ParentChildLink(
			long otherChildId,
			@Schema(description = "Mot court du lien, par exemple « Frère ».")
			String label) {
	}

	@Schema(name = "ParentSchoolOption")
	public record ParentSchoolOption(long id, String name, String country, List<String> grades) {
	}

	@Schema(name = "CreateParentChildRequest")
	public record CreateParentChildRequest(
			String firstName,
			String lastName,
			@Schema(description = "GARCON, FILLE ou NON_GENRE")
			String sex,
			String age,
			String country,
			String grade,
			Long schoolId,
			String avatarCode,
			String photoBase64,
			String photoContentType,
			String pin,
			String pinConfirmation,
			@Schema(description = "Enfant de départ. Absent pour un formulaire vide.")
			Long fromChildId,
			@Schema(description = "AUCUN, FRERE, SOEUR, DEMI_FRERE, DEMI_SOEUR, JUMEAU, JUMELLE, COUSIN ou COUSINE.")
			String link) {
	}
}
