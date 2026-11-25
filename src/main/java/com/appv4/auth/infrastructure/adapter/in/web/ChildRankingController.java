package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ReadChildRankingUseCase;
import com.appv4.auth.application.port.out.ChildRankingRosterPort.ChildPhoto;
import com.appv4.auth.application.result.ChildRankingView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Classement de l'enfant connecté (BACK-22).
 *
 * <p><b>Business :</b> cinq groupes, dix cartes, et la sienne si elle est plus loin.
 * <p><b>Technique :</b> {@code GET /api/ext/ranking} avec le jeton enfant.
 * <p><b>Métier / flow :</b> adapter in → {@link ReadChildRankingUseCase}.
 */
@RestController
@RequestMapping("/api/ext/ranking")
@Tag(name = "Enfant — classement")
public class ChildRankingController {

	private final ReadChildRankingUseCase ranking;

	public ChildRankingController(ReadChildRankingUseCase ranking) {
		this.ranking = ranking;
	}

	@GetMapping
	@Operation(operationId = "readChildRanking", summary = "Lire un classement de l'enfant connecté")
	@ApiResponse(responseCode = "200", description = "Cartes, pastilles et phrase éventuelle")
	@ApiResponse(
			responseCode = "400",
			description = "Groupe inconnu",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildRankingResponse> read(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestParam(required = false) String scope) {
		ChildRankingView view = ranking.read(bearer(authorization), scope);
		return ResponseEntity.ok(toResponse(view));
	}

	@GetMapping("/photo")
	@Operation(operationId = "readChildRankingPhoto", summary = "Lire la photo d'un enfant du classement")
	@ApiResponse(responseCode = "200", description = "Octets de la photo d'un membre du groupe")
	@ApiResponse(
			responseCode = "404",
			description = "Pas de photo, ou enfant hors du groupe",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<byte[]> photo(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestParam(required = false) String scope,
			@RequestParam long childId) {
		ChildPhoto photo = ranking.photo(bearer(authorization), scope, childId);
		MediaType type = photo.contentType() == null || photo.contentType().isBlank()
				? MediaType.APPLICATION_OCTET_STREAM
				: MediaType.parseMediaType(photo.contentType());
		return ResponseEntity.ok().contentType(type).body(photo.bytes());
	}

	private static ChildRankingResponse toResponse(ChildRankingView view) {
		List<ChildRankingScopeResponse> scopes = view.scopes().stream()
				.map(chip -> new ChildRankingScopeResponse(chip.code(), chip.label(), chip.selected()))
				.toList();
		List<ChildRankingCardResponse> cards = view.cards().stream()
				.map(card -> new ChildRankingCardResponse(
						card.place(),
						card.firstName(),
						card.xp(),
						card.mine(),
						card.pinned(),
						card.face(),
						card.avatarCode(),
						card.styleCode(),
						card.options(),
						card.pins().stream()
								.map(pin -> new ChildRankingPinResponse(pin.zoneCode(), pin.variantCode()))
								.toList(),
						card.childId()))
				.toList();
		return new ChildRankingResponse(scopes, view.scope(), view.groupLabel(), view.phrase(), cards);
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}

	@Schema(name = "ChildRankingResponse")
	public record ChildRankingResponse(
			List<ChildRankingScopeResponse> scopes,
			String scope,
			String groupLabel,
			String phrase,
			List<ChildRankingCardResponse> cards) {
	}

	@Schema(name = "ChildRankingScopeResponse")
	public record ChildRankingScopeResponse(String code, String label, boolean selected) {
	}

	@Schema(name = "ChildRankingCardResponse")
	public record ChildRankingCardResponse(
			int place,
			String firstName,
			int xp,
			boolean mine,
			boolean pinned,
			@Schema(description = "personnage, photo, animal ou aucun")
			String face,
			String avatarCode,
			String styleCode,
			java.util.Map<String, Object> options,
			List<ChildRankingPinResponse> pins,
			long childId) {
	}

	@Schema(name = "ChildRankingPinResponse")
	public record ChildRankingPinResponse(String zoneCode, String variantCode) {
	}
}
