package com.appv4.auth.application.result;

import java.util.List;
import java.util.Map;

/**
 * Personnage lu pour Profil (BACK-48).
 *
 * <p><b>Business :</b> l'enfant voit Lorelei tant qu'il n'a rien enregistré.
 * <p><b>Technique :</b> record avant le JSON {@code GET /api/ext/avatar}.
 * <p><b>Métier / flow :</b> résultat de {@link com.appv4.auth.application.port.in.ReadChildAvatarUseCase}.
 */
public record ChildAvatarView(
		String seed,
		String styleCode,
		String styleName,
		String presetId,
		String credit,
		List<StyleCard> styles,
		List<PresetCard> presets,
		List<ZoneCard> zones) {

	public ChildAvatarView {
		styles = styles == null ? List.of() : List.copyOf(styles);
		presets = presets == null ? List.of() : List.copyOf(presets);
		zones = zones == null ? List.of() : List.copyOf(zones);
	}

	public record StyleCard(
			String code,
			String name,
			String credit,
			List<String> zoneCodes,
			List<StylePreset> presets) {
		public StyleCard {
			zoneCodes = zoneCodes == null ? List.of() : List.copyOf(zoneCodes);
			presets = presets == null ? List.of() : List.copyOf(presets);
		}
	}

	public record StylePreset(String id, String name, Map<String, Object> options) {
		public StylePreset {
			options = options == null ? Map.of() : Map.copyOf(options);
		}
	}

	public record PresetCard(String id, String name, Map<String, Object> options) {
	}

	public record ZoneCard(
			String code,
			String label,
			int seuil,
			String thresholdLabel,
			String missingLabel,
			boolean open,
			List<VariantCard> variants,
			String selectedCode) {
		public ZoneCard {
			variants = variants == null ? List.of() : List.copyOf(variants);
		}
	}

	public record VariantCard(String code, String label) {
	}
}
