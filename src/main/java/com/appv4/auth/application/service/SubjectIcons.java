package com.appv4.auth.application.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import com.appv4.auth.application.result.AvatarChoice;

/**
 * Codes d'icônes des matières (BACK-15 / BACK-16).
 *
 * <p><b>Business :</b> le professeur choisit un dessin pour sa matière ; l'enfant
 * le voit sur Learn. Recherche côté admin via l'API Iconify (centaines de milliers
 * d'icônes) ; le back n'autorise que le format, pas une liste figée.
 * <p><b>Technique :</b> legacy court ({@code math}, …) encore accepté, ou
 * {@code prefix:name} Iconify (max 128). Suggestions = exemples populaires.
 * <p><b>Métier / flow :</b> create/setIcon → {@code icon_code} → GET child / Learn.
 */
public final class SubjectIcons {

	/** Anciens codes emoji gardés pour les matières déjà en base. */
	private static final Set<String> LEGACY = Set.of(
			"math", "numbers", "counting", "geometry", "shapes", "fractions", "algebra",
			"calculator", "clock", "measure", "money", "alphabet", "letters", "reading",
			"writing", "words", "vocabulary", "grammar", "spelling", "poetry", "story",
			"books", "library", "language", "english", "dutch", "spanish", "german",
			"science", "nature", "plants", "animals", "insects", "ocean", "space",
			"weather", "earth", "globe", "map", "history", "geography", "civics",
			"knowledge", "discovery", "experiment", "lab", "music", "singing",
			"instrument", "art", "drawing", "painting", "craft", "colors", "sport",
			"football", "basketball", "swim", "dance", "yoga", "run", "bike", "body",
			"health", "food", "fruit", "vegetables", "kitchen", "home", "family",
			"friends", "emotions", "opposites", "seasons", "calendar", "school",
			"pencil", "backpack", "computer", "tablet", "robot", "code", "puzzle",
			"memory", "logic", "chess", "games", "trophy", "star", "heart", "flag",
			"transport", "car", "train", "plane", "boat", "city", "farm", "forest",
			"mountain", "beach", "fire", "water", "air", "recycle", "light", "sound",
			"touch", "sight", "smell", "taste");

	/**
	 * Iconify : {@code prefix:name} (lettres, chiffres, tirets).
	 * Ex. {@code fluent-emoji:open-book}, {@code mdi:calculator}.
	 */
	private static final Pattern ICONIFY = Pattern.compile(
			"^[a-z0-9]+(?:-[a-z0-9]+)*:[a-z0-9]+(?:-[a-z0-9]+)*$");

	private static final int MAX_LEN = 128;

	/** Suggestions affichées avant recherche (pas une liste fermée). */
	public static final List<AvatarChoice> CHOICES = List.of(
			new AvatarChoice("fluent-emoji:abacus", "Boulier / maths"),
			new AvatarChoice("fluent-emoji:open-book", "Livre ouvert"),
			new AvatarChoice("fluent-emoji:books", "Livres"),
			new AvatarChoice("fluent-emoji:pencil", "Crayon"),
			new AvatarChoice("fluent-emoji:globe-showing-europe-africa", "Globe"),
			new AvatarChoice("fluent-emoji:microscope", "Microscope"),
			new AvatarChoice("fluent-emoji:artist-palette", "Palette"),
			new AvatarChoice("fluent-emoji:musical-notes", "Musique"),
			new AvatarChoice("fluent-emoji:soccer-ball", "Sport"),
			new AvatarChoice("fluent-emoji:lion", "Lion"),
			new AvatarChoice("fluent-emoji:light-bulb", "Idée"),
			new AvatarChoice("fluent-emoji:rocket", "Fusée"),
			new AvatarChoice("mdi:calculator", "Calculatrice"),
			new AvatarChoice("mdi:alphabet-latin", "Alphabet"),
			new AvatarChoice("mdi:map", "Carte"),
			new AvatarChoice("mdi:school", "École"));

	private SubjectIcons() {
	}

	/**
	 * Code normalisé, ou empty si la chaîne est vide (pas d'icône).
	 * Empty aussi si le format est refusé — l'appelant refuse alors la requête.
	 */
	public static Optional<String> normalizeKnown(String raw) {
		if (raw == null || raw.isBlank()) {
			return Optional.empty();
		}
		String code = raw.trim().toLowerCase(Locale.ROOT);
		if (code.length() > MAX_LEN) {
			return Optional.empty();
		}
		if (LEGACY.contains(code)) {
			return Optional.of(code);
		}
		if (ICONIFY.matcher(code).matches()) {
			return Optional.of(code);
		}
		return Optional.empty();
	}

	/** Vrai si absent, vide, ou code accepté (legacy ou Iconify). */
	public static boolean isBlankOrKnown(String raw) {
		if (raw == null || raw.isBlank()) {
			return true;
		}
		return normalizeKnown(raw).isPresent();
	}
}
