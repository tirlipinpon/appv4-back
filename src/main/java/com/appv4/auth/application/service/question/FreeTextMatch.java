package com.appv4.auth.application.service.question;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Comparaison d'une réponse libre (SCRUM-198).
 *
 * <p><b>Business :</b> « paris », « PARIS » et « Pâris » avec des espaces au bord
 * comptent comme « Paris ». « Lyon », « Parix » et « Pa ris » ne comptent pas.
 * Un champ vide n'est pas une réussite.
 * <p><b>Technique :</b> retire les espaces au bord, passe en minuscules, enlève
 * les accents. Un espace au milieu reste.
 * <p><b>Métier / flow :</b> POST attempts de la forme {@code texte}, via
 * {@link com.appv4.auth.application.service.question.play.FreeTextChildAnswerGrader}.
 */
public final class FreeTextMatch {
	private static final Pattern EDGE_SPACES = Pattern.compile("^[\\p{Z}\\x00-\\x20]+|[\\p{Z}\\x00-\\x20]+$");

	private FreeTextMatch() {
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
		String trimmed = value == null ? "" : EDGE_SPACES.matcher(value).replaceAll("").toLowerCase(Locale.ROOT);
		String decomposed = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
		return decomposed.replaceAll("\\p{M}+", "");
	}
}
