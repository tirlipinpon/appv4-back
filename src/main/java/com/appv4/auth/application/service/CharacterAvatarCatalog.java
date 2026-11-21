package com.appv4.auth.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Familles Characters figées, DiceBear 10.6 (BACK-48).
 *
 * <p><b>Business :</b> une famille publiée plus tard n'entre pas toute seule.
 * <p><b>Technique :</b> JSON de classpath, sans les dessins.
 * <p><b>Métier / flow :</b> la lecture d'avatar s'appuie sur ce catalogue.
 */
public final class CharacterAvatarCatalog {

	private final List<Style> styles;

	private CharacterAvatarCatalog(List<Style> styles) {
		this.styles = List.copyOf(styles);
	}

	public static CharacterAvatarCatalog load() {
		ObjectMapper mapper = new ObjectMapper();
		try (InputStream in = CharacterAvatarCatalog.class.getResourceAsStream("/avatar/characters-10.6.json")) {
			if (in == null) {
				throw new IllegalStateException("Catalogue d'avatar introuvable.");
			}
			JsonNode root = mapper.readTree(in);
			List<Style> styles = new ArrayList<>();
			for (JsonNode style : root.get("styles")) {
				styles.add(readStyle(mapper, style));
			}
			return new CharacterAvatarCatalog(styles);
		} catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

	public List<Style> styles() {
		return styles;
	}

	public Style byCode(String code) {
		return styles.stream()
				.filter(style -> style.code().equals(code))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Style absent du catalogue : " + code));
	}

	@SuppressWarnings("unchecked")
	private static Style readStyle(ObjectMapper mapper, JsonNode style) {
		List<Zone> zones = new ArrayList<>();
		for (JsonNode zone : style.get("zones")) {
			List<String> variants = new ArrayList<>();
			for (JsonNode variant : zone.get("variants")) {
				variants.add(variant.asText());
			}
			zones.add(new Zone(zone.get("code").asText(), List.copyOf(variants)));
		}
		List<Preset> presets = new ArrayList<>();
		for (JsonNode preset : style.get("presets")) {
			presets.add(new Preset(
					preset.get("id").asText(),
					preset.get("name").asText(),
					mapper.convertValue(preset.get("options"), Map.class)));
		}
		String license = style.get("license").asText();
		String author = style.get("author").asText();
		String credit = license.contains("CC BY") ? author + " — CC BY 4.0" : null;
		return new Style(
				style.get("code").asText(),
				style.get("name").asText(),
				credit,
				List.copyOf(zones),
				List.copyOf(presets));
	}

	public record Style(String code, String name, String credit, List<Zone> zones, List<Preset> presets) {
	}

	public record Zone(String code, List<String> variants) {
	}

	public record Preset(String id, String name, Map<String, Object> options) {
		public Preset {
			options = options == null ? Map.of() : new LinkedHashMap<>(options);
		}
	}
}
