package com.appv4.auth.application.service;

import java.util.List;

/**
 * Lien fermé entre deux enfants (SCRUM-158).
 *
 * <p><b>Business :</b> frère et sœur, demi, jumeaux ou cousins. Le mot affiché suit le sexe
 * de l'enfant dont on parle. Un garçon qui a une sœur est, pour elle, son frère.
 * « Aucun » n'enregistre rien. Pas de texte libre.
 * <p><b>Technique :</b> le code du formulaire choisit la famille (frère/sœur, demi, jumeau, cousin).
 * Le sexe de la fiche choisit le mot.
 * <p><b>Métier / flow :</b> choisi à l'ajout ou à la modification, relu avec la liste.
 */
public final class ChildKinship {

	public static final String NONE = "AUCUN";

	public static final List<String> CODES = List.of(
			"FRERE",
			"SOEUR",
			"DEMI_FRERE",
			"DEMI_SOEUR",
			"JUMEAU",
			"JUMELLE",
			"COUSIN",
			"COUSINE");

	private ChildKinship() {
	}

	/** Vrai pour vide ou {@code AUCUN} : on ne crée pas de ligne de lien. */
	public static boolean isNone(String raw) {
		return raw == null || raw.isBlank() || NONE.equals(raw.trim());
	}

	public static boolean isKnown(String raw) {
		return raw != null && CODES.contains(raw.trim());
	}

	/**
	 * Phrase qui décrit une personne, par exemple « Akira est le frère de Léa ».
	 *
	 * <p><b>Business :</b> le verbe suit le sexe de la personne citée en premier, pas celui de l'autre.
	 */
	public static String phrase(String describedFirstName, String describedSex, String code, String otherFirstName) {
		return describedFirstName + " " + wording(code, describedSex) + " " + otherFirstName;
	}

	/**
	 * Mot court à côté du portrait, par exemple « Frère ».
	 *
	 * <p><b>Business :</b> c'est le sexe de cet enfant-là. Non genré reste neutre.
	 */
	public static String word(String code, String sex) {
		String family = family(code);
		String normalized = sex == null || sex.isBlank() ? ChildSex.UNSPECIFIED : sex.trim();
		return switch (family) {
			case "SIBLING" -> switch (normalized) {
				case ChildSex.BOY -> "Frère";
				case ChildSex.GIRL -> "Sœur";
				default -> "Frère/sœur";
			};
			case "HALF" -> switch (normalized) {
				case ChildSex.BOY -> "Demi-frère";
				case ChildSex.GIRL -> "Demi-sœur";
				default -> "Demi-frère/sœur";
			};
			case "TWIN" -> switch (normalized) {
				case ChildSex.BOY -> "Jumeau";
				case ChildSex.GIRL -> "Jumelle";
				default -> "Jumeau/jumelle";
			};
			case "COUSIN" -> switch (normalized) {
				case ChildSex.BOY -> "Cousin";
				case ChildSex.GIRL -> "Cousine";
				default -> "Cousin/cousine";
			};
			default -> throw new IllegalArgumentException(code);
		};
	}

	private static String wording(String code, String sex) {
		String family = family(code);
		String normalized = sex == null || sex.isBlank() ? ChildSex.UNSPECIFIED : sex.trim();
		return switch (family) {
			case "SIBLING" -> switch (normalized) {
				case ChildSex.BOY -> "est le frère de";
				case ChildSex.GIRL -> "est la sœur de";
				default -> "est le frère ou la sœur de";
			};
			case "HALF" -> switch (normalized) {
				case ChildSex.BOY -> "est le demi-frère de";
				case ChildSex.GIRL -> "est la demi-sœur de";
				default -> "est le demi-frère ou la demi-sœur de";
			};
			case "TWIN" -> switch (normalized) {
				case ChildSex.BOY -> "est le jumeau de";
				case ChildSex.GIRL -> "est la jumelle de";
				default -> "est le jumeau ou la jumelle de";
			};
			case "COUSIN" -> switch (normalized) {
				case ChildSex.BOY -> "est le cousin de";
				case ChildSex.GIRL -> "est la cousine de";
				default -> "est le cousin ou la cousine de";
			};
			default -> throw new IllegalArgumentException(code);
		};
	}

	private static String family(String code) {
		if (code == null) {
			throw new IllegalArgumentException("null");
		}
		return switch (code) {
			case "FRERE", "SOEUR" -> "SIBLING";
			case "DEMI_FRERE", "DEMI_SOEUR" -> "HALF";
			case "JUMEAU", "JUMELLE" -> "TWIN";
			case "COUSIN", "COUSINE" -> "COUSIN";
			default -> throw new IllegalArgumentException(code);
		};
	}
}
