package com.appv4.auth.application.service.question;

import java.util.Locale;

/**
 * Comparaison d'une saisie lettre par lettre (SCRUM-182).
 *
 * <p><b>Business :</b> « paris » et « PARIS », avec des espaces au bord, comptent
 * comme « Paris ». « Parïs », « Parix » et « Lyon » ne comptent pas.
 * Un champ vide n'est pas une réussite. L'accent n'est pas effacé.
 * <p><b>Technique :</b> retire les espaces au bord et passe en minuscules.
 * Un espace au milieu et un accent restent.
 * <p><b>Métier / flow :</b> appelée quand l'élève appuie sur Valider, une fois
 * toutes les cases remplies.
 */
public final class LetterByLetterMatch {

	private LetterByLetterMatch() {
	}

	public static boolean accepted(String expected, String attempt) {
		if (attempt == null || attempt.isBlank()) {
			return false;
		}
		String stored = normalize(expected);
		if (stored.isEmpty()) {
			return false;
		}
		return stored.equals(normalize(attempt));
	}

	static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
