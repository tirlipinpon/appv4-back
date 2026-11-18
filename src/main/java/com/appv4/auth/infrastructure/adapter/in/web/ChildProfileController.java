package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ReadChildProfileUseCase;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildPhotoBytes;
import com.appv4.auth.application.result.ChildProfileView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Profil de l'enfant connecté (BACK-14).
 *
 * <p><b>Business :</b> l'accueil learning affiche le prénom et l'image de la fiche.
 * <p><b>Technique :</b> {@code GET /api/ext/me} et {@code GET /api/ext/me/photo} avec Bearer enfant.
 * <p><b>Métier / flow :</b> adapter in → {@link ReadChildProfileUseCase}.
 */
@RestController
@RequestMapping("/api/ext/me")
@Tag(name = "Enfant — profil")
public class ChildProfileController {

	private final ReadChildProfileUseCase readChildProfileUseCase;

	public ChildProfileController(ReadChildProfileUseCase readChildProfileUseCase) {
		this.readChildProfileUseCase = readChildProfileUseCase;
	}

	@GetMapping
	@Operation(operationId = "readChildProfile", summary = "Lire le profil de l'enfant connecté")
	@ApiResponse(responseCode = "200", description = "Profil de la fiche active")
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildProfileResponse> me(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		ChildProfileView view = readChildProfileUseCase.profile(bearer(authorization));
		return ResponseEntity.ok(new ChildProfileResponse(
				view.childId(), view.firstName(), view.avatarCode(), view.hasPhoto()));
	}

	@GetMapping("/photo")
	@Operation(operationId = "readChildProfilePhoto", summary = "Lire la photo de l'enfant connecté")
	@ApiResponse(responseCode = "200", description = "Octets de la photo")
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "404",
			description = "Pas de photo sur la fiche",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<byte[]> photo(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		ChildPhotoBytes photo = readChildProfileUseCase.photo(bearer(authorization));
		MediaType type = photo.contentType() == null || photo.contentType().isBlank()
				? MediaType.APPLICATION_OCTET_STREAM
				: MediaType.parseMediaType(photo.contentType());
		return ResponseEntity.ok().contentType(type).body(photo.bytes());
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}

	@Schema(name = "ChildProfileResponse")
	public record ChildProfileResponse(long childId, String firstName, String avatarCode, boolean hasPhoto) {
	}
}
