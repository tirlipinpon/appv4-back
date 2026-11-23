package com.appv4.auth.application.service;

import java.util.Collection;
import java.util.HashSet;
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

	/**
	 * Palette posée toute seule à la création d'une matière.
	 *
	 * <p><b>Business :</b> le professeur n'a pas à choisir une couleur ni une icône.
	 * Deux matières visibles dans la même classe ne partagent ni l'une ni l'autre.
	 * <p><b>Technique :</b> chaque entrée a un code déjà accepté et une teinte {@code #rrggbb} unique.
	 * <p><b>Métier / flow :</b> {@code create} sans icône → première entrée encore libre → {@code icon_code}.
	 */
	public record SubjectLook(String iconCode, String accentColor) {
	}

	private static final List<SubjectLook> PALETTE = List.of(
			new SubjectLook("icons8:13216", "#3949ab"),
			new SubjectLook("icons8:4210", "#6d4c41"),
			new SubjectLook("icons8:10404", "#00897b"),
			new SubjectLook("icons8:2926", "#00838f"),
			new SubjectLook("icons8:10829", "#ad1457"),
			new SubjectLook("icons8:1656", "#2e7d32"),
			new SubjectLook("icons8:1599", "#ef6c00"),
			new SubjectLook("icons8:13806", "#1565c0"),
			new SubjectLook("icons8:4285", "#5e35b1"),
			new SubjectLook("icons8:11441", "#f9a825"),
			new SubjectLook("icons8:435", "#455a64"),
			new SubjectLook("icons8:10828", "#c2185b"),
			new SubjectLook("math", "#7e57c2"),
			new SubjectLook("books", "#8d6e63"),
			new SubjectLook("science", "#26a69a"),
			new SubjectLook("alphabet", "#ffb300"),
			new SubjectLook("reading", "#5c6bc0"),
			new SubjectLook("writing", "#00796b"),
			new SubjectLook("animals", "#689f38"),
			new SubjectLook("music", "#d81b60"),
			new SubjectLook("sport", "#43a047"),
			new SubjectLook("art", "#fb8c00"),
			new SubjectLook("globe", "#0277bd"),
			new SubjectLook("school", "#512da8"),
			new SubjectLook("knowledge", "#0097a7"),
			new SubjectLook("pencil", "#795548"),
			new SubjectLook("numbers", "#303f9f"),
			new SubjectLook("words", "#c0ca33"),
			new SubjectLook("opposites", "#8e24aa"),
			new SubjectLook("history", "#5d4037"));

	static {
		Set<String> icons = new HashSet<>();
		Set<String> colors = new HashSet<>();
		for (SubjectLook look : PALETTE) {
			if (!normalizeKnown(look.iconCode()).orElse("").equals(look.iconCode()) || !icons.add(look.iconCode())) {
				throw new IllegalStateException("Icône de matière en double ou inconnue : " + look.iconCode());
			}
			if (look.accentColor() == null || !look.accentColor().matches("^#[0-9a-f]{6}$")
					|| !colors.add(look.accentColor())) {
				throw new IllegalStateException("Couleur de matière en double ou invalide.");
			}
		}
	}

	/** Teinte de cette icône, ou null si elle n'est pas dans la palette. */
	public static String accentOf(String iconCode) {
		if (iconCode == null || iconCode.isBlank()) {
			return null;
		}
		for (SubjectLook look : PALETTE) {
			if (look.iconCode().equals(iconCode)) {
				return look.accentColor();
			}
		}
		return null;
	}

	/**
	 * Première icône de la palette dont le code et la teinte ne sont pas déjà pris.
	 * Null si la palette de la classe est épuisée : on n'en réutilise pas une.
	 */
	public static String pickFree(Collection<String> usedIconCodes) {
		Set<String> usedIcons = new HashSet<>();
		Set<String> usedColors = new HashSet<>();
		if (usedIconCodes != null) {
			for (String code : usedIconCodes) {
				if (code == null || code.isBlank()) {
					continue;
				}
				usedIcons.add(code);
				String color = accentOf(code);
				if (color != null) {
					usedColors.add(color);
				}
			}
		}
		for (SubjectLook look : PALETTE) {
			if (usedIcons.contains(look.iconCode()) || usedColors.contains(look.accentColor())) {
				continue;
			}
			return look.iconCode();
		}
		return null;
	}
}
