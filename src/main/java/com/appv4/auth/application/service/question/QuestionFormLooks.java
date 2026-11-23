package com.appv4.auth.application.service.question;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Couleur et icône fixes de chaque forme de question déjà jouable.
 *
 * <p><b>Business :</b> le professeur ne choisit pas l'identité visuelle d'un jeu.
 * Chaque forme a la sienne, différente des autres, pour la reconnaître d'un coup d'œil.
 * <p><b>Technique :</b> code d'icône stable (dessin côté écran) et couleur {@code #rrggbb}.
 * Une forme inconnue n'a pas d'identité : l'écran n'en invente pas.
 * <p><b>Métier / flow :</b> catalogue des formes → {@code GET question-forms} et liste enfant.
 */
public final class QuestionFormLooks {

	public record Look(String iconCode, String color) {
	}

	private static final Pattern COLOR = Pattern.compile("^#[0-9a-f]{6}$");

	private static final Map<String, Look> LOOKS = Map.copyOf(new LinkedHashMap<>(Map.ofEntries(
			Map.entry("qcm", new Look("checklist", "#4c6fff")),
			Map.entry("memory", new Look("cards", "#e85d8c")),
			Map.entry("order", new Look("sort", "#f5a524")),
			Map.entry("vrai_faux", new Look("true-false", "#2bb673")),
			Map.entry("liens", new Look("link", "#0e8fbf")),
			Map.entry("case_vide", new Look("gap", "#8b6cff")),
			Map.entry("classement", new Look("columns", "#8a5a2b")),
			Map.entry("simon", new Look("simon", "#e24b4b")),
			Map.entry("image_interactive", new Look("image-click", "#c44bd9")),
			Map.entry("reponse_libre", new Look("letters", "#f07a3a")),
			Map.entry("puzzle", new Look("puzzle", "#6b8f3a")),
			Map.entry("texte", new Look("text", "#1aa6a6")))));

	static {
		Set<String> icons = new HashSet<>();
		Set<String> colors = new HashSet<>();
		for (Look look : LOOKS.values()) {
			if (look.iconCode() == null || look.iconCode().isBlank() || !icons.add(look.iconCode())) {
				throw new IllegalStateException("Icône de forme en double ou vide.");
			}
			if (look.color() == null || !COLOR.matcher(look.color()).matches() || !colors.add(look.color())) {
				throw new IllegalStateException("Couleur de forme en double ou invalide.");
			}
		}
	}

	private QuestionFormLooks() {
	}

	public static Set<String> codes() {
		return LOOKS.keySet();
	}

	/** Identité de la forme, ou null si ce code n'est pas un jeu connu. */
	public static Look of(String formCode) {
		if (formCode == null) {
			return null;
		}
		return LOOKS.get(formCode);
	}
}
