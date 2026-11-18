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
 * <p><b>Business :</b> le professeur cherche une icône en français (API Icons8
 * multilangue) ; l'enfant la voit sur Learn.
 * <p><b>Technique :</b> {@code icons8:{id}} (recherche FR), plus legacy court
 * et anciens codes Iconify encore acceptés. Max 128 car.
 * <p><b>Métier / flow :</b> create/setIcon → {@code icon_code} → GET child / Learn.
 */
public final class SubjectIcons {

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

	/** Icons8 : {@code icons8:13216} (id API). */
	private static final Pattern ICONS8 = Pattern.compile("^icons8:[a-zA-Z0-9_-]{1,64}$");

	/** Anciens codes Iconify éventuels : {@code prefix:name}. */
	private static final Pattern ICONIFY = Pattern.compile(
			"^[a-z0-9]+(?:-[a-z0-9]+)*:[a-z0-9]+(?:-[a-z0-9]+)*$");

	private static final int MAX_LEN = 128;

	/** Suggestions de démarrage (libellés FR, ids Icons8 color). */
	public static final List<AvatarChoice> CHOICES = List.of(
			new AvatarChoice("icons8:13216", "Mathématiques"),
			new AvatarChoice("icons8:4210", "Livre"),
			new AvatarChoice("icons8:10404", "Chimie"),
			new AvatarChoice("icons8:2926", "Microscope"),
			new AvatarChoice("icons8:10829", "Musique"),
			new AvatarChoice("icons8:1656", "Sport"),
			new AvatarChoice("icons8:1599", "Palette"),
			new AvatarChoice("icons8:13806", "Globe"),
			new AvatarChoice("icons8:4285", "École"),
			new AvatarChoice("icons8:11441", "Crayon"),
			new AvatarChoice("icons8:435", "Calculatrice"),
			new AvatarChoice("icons8:10828", "Notes de musique"));

	private SubjectIcons() {
	}

	public static Optional<String> normalizeKnown(String raw) {
		if (raw == null || raw.isBlank()) {
			return Optional.empty();
		}
		String code = raw.trim();
		if (code.length() > MAX_LEN) {
			return Optional.empty();
		}
		// Icons8 : l'id est sensible à la casse (ex. cIvMqIm3AWTj).
		if (code.length() > 7 && code.regionMatches(true, 0, "icons8:", 0, 7)) {
			String id = code.substring(7);
			String normalized = "icons8:" + id;
			if (ICONS8.matcher(normalized).matches()) {
				return Optional.of(normalized);
			}
			return Optional.empty();
		}
		String lower = code.toLowerCase(Locale.ROOT);
		if (LEGACY.contains(lower)) {
			return Optional.of(lower);
		}
		if (ICONIFY.matcher(lower).matches()) {
			return Optional.of(lower);
		}
		return Optional.empty();
	}

	public static boolean isBlankOrKnown(String raw) {
		if (raw == null || raw.isBlank()) {
			return true;
		}
		return normalizeKnown(raw).isPresent();
	}
}
