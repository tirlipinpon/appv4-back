package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.ReadChildAvatarUseCase;
import com.appv4.auth.application.result.ChildAvatarView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Lecture du personnage pour Profil (BACK-48).
 *
 * <p><b>Business :</b> l'enfant voit son avatar, pas son image de profil.
 * <p><b>Technique :</b> {@code GET /api/ext/avatar} avec Bearer enfant.
 * <p><b>Métier / flow :</b> adapter in → {@link ReadChildAvatarUseCase}.
 */
@RestController
@RequestMapping("/api/ext/avatar")
@Tag(name = "Enfant — avatar")
public class ChildAvatarController {

	private final ReadChildAvatarUseCase readChildAvatarUseCase;

	public ChildAvatarController(ReadChildAvatarUseCase readChildAvatarUseCase) {
		this.readChildAvatarUseCase = readChildAvatarUseCase;
	}

	@GetMapping
	@Operation(operationId = "readChildAvatar", summary = "Lire le personnage de l'enfant connecté")
	@ApiResponse(responseCode = "200", description = "Personnage, styles, palettes et zones")
	@ApiResponse(
			responseCode = "401",
			description = "Badge absent, adulte, expiré, ou fiche arrêtée",
			content = @Content(schema = @Schema(implementation = AuthExceptionHandler.ErrorBody.class)))
	public ResponseEntity<ChildAvatarResponse> avatar(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
		return ResponseEntity.ok(ChildAvatarResponse.from(readChildAvatarUseCase.avatar(bearer(authorization))));
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}

	@Schema(name = "ChildAvatarResponse")
	public record ChildAvatarResponse(
			String seed,
			String styleCode,
			String styleName,
			@Schema(nullable = true) String presetId,
			@Schema(nullable = true) String credit,
			List<ChildAvatarStyleCard> styles,
			List<ChildAvatarPresetCard> presets,
			List<ChildAvatarZoneCard> zones) {

		static ChildAvatarResponse from(ChildAvatarView view) {
			return new ChildAvatarResponse(
					view.seed(),
					view.styleCode(),
					view.styleName(),
					view.presetId(),
					view.credit(),
					view.styles().stream().map(ChildAvatarStyleCard::from).toList(),
					view.presets().stream().map(ChildAvatarPresetCard::from).toList(),
					view.zones().stream().map(ChildAvatarZoneCard::from).toList());
		}
	}

	@Schema(name = "ChildAvatarStyleCard")
	public record ChildAvatarStyleCard(
			String code,
			String name,
			@Schema(nullable = true) String credit,
			List<String> zoneCodes) {

		static ChildAvatarStyleCard from(ChildAvatarView.StyleCard style) {
			return new ChildAvatarStyleCard(style.code(), style.name(), style.credit(), style.zoneCodes());
		}
	}

	@Schema(name = "ChildAvatarPresetCard")
	public record ChildAvatarPresetCard(String id, String name, Map<String, Object> options) {

		static ChildAvatarPresetCard from(ChildAvatarView.PresetCard preset) {
			return new ChildAvatarPresetCard(preset.id(), preset.name(), preset.options());
		}
	}

	@Schema(name = "ChildAvatarZoneCard")
	public record ChildAvatarZoneCard(
			String code,
			String label,
			int seuil,
			@Schema(nullable = true) String thresholdLabel,
			@Schema(nullable = true) String missingLabel,
			boolean open,
			List<ChildAvatarVariantCard> variants) {

		static ChildAvatarZoneCard from(ChildAvatarView.ZoneCard zone) {
			return new ChildAvatarZoneCard(
					zone.code(),
					zone.label(),
					zone.seuil(),
					zone.thresholdLabel(),
					zone.missingLabel(),
					zone.open(),
					zone.variants().stream().map(ChildAvatarVariantCard::from).toList());
		}
	}

	@Schema(name = "ChildAvatarVariantCard")
	public record ChildAvatarVariantCard(String code, String label) {

		static ChildAvatarVariantCard from(ChildAvatarView.VariantCard variant) {
			return new ChildAvatarVariantCard(variant.code(), variant.label());
		}
	}
}
