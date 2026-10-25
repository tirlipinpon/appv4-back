package com.appv4.auth.application.service;

import java.util.List;
import java.util.Map;

/**
 * Lien fermé entre le nouvel enfant et l'enfant de départ (SCRUM-158).
 *
 * <p><b>Business :</b> les deux fiches affichent la même phrase. « Aucun » n'enregistre rien.
 * Frère, sœur, cousin et le reste de la liste : rien d'autre, pas de texte libre.
 * <p><b>Technique :</b> codes stables du formulaire. La phrase est reconstruite à la lecture.
 * <p><b>Métier / flow :</b> choisi au moment d'ajouter un enfant à partir d'un autre, puis relu avec la liste.
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

	private static final Map<String, String> WORDING = Map.of(
			"FRERE", "est le frère de",
			"SOEUR", "est la sœur de",
			"DEMI_FRERE", "est le demi-frère de",
			"DEMI_SOEUR", "est la demi-sœur de",
			"JUMEAU", "est le jumeau de",
			"JUMELLE", "est la jumelle de",
			"COUSIN", "est le cousin de",
			"COUSINE", "est la cousine de");

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
	 * Phrase affichée sur les deux fiches, par exemple « Noah est le frère de Léa ».
	 */
	public static String phrase(String subjectFirstName, String code, String relatedFirstName) {
		String wording = WORDING.get(code);
		if (wording == null) {
			throw new IllegalArgumentException(code);
		}
		return subjectFirstName + " " + wording + " " + relatedFirstName;
	}
}
