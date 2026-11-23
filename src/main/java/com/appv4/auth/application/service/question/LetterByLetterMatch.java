package com.appv4.auth.application.service.question;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Comparaison d'une saisie lettre par lettre (SCRUM-182).
 *
 * <p><b>Business :</b> « paris » et « PARIS », avec des espaces au bord, comptent
 * comme « Paris ». « Parïs », « Parix » et « Lyon » ne comptent pas.
 * Un champ vide n'est pas une réussite. L'accent n'est pas effacé.
 * Un espace au milieu du mot (ex. « tire bouchon ») est une case sautée :
 * l'enfant ne le tape pas, il passe au mot suivant.
 * Une lettre déjà juste se verrouille (plus d'effacement).
 * <p><b>Technique :</b> retire les espaces au bord et passe en minuscules pour
 * le verdict final. Le masque et les verrous gardent la longueur du mot stocké.
 * <p><b>Métier / flow :</b> masque au GET play ; verrous au fil de la saisie ;
 * {@link #accepted} à Valider.
 */
public final class LetterByLetterMatch {

	/** Marqueur d'une case lettre encore vide dans une tentative partielle. */
	public static final char EMPTY_SLOT = '\0';

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

	/**
	 * Masque jouable : {@code #} = case lettre, espace = saut (sans révéler le mot).
	 */
	public static String mask(String expected) {
		if (expected == null || expected.isEmpty()) {
			return "";
		}
		StringBuilder out = new StringBuilder(expected.length());
		for (int i = 0; i < expected.length(); i++) {
			char ch = expected.charAt(i);
			out.append(Character.isWhitespace(ch) ? ' ' : '#');
		}
		return out.toString();
	}

	/**
	 * Verrou par position : espace toujours verrouillé ; lettre verrouillée si
	 * remplie et identique (casse ignorée, accent gardé).
	 */
	public static List<Boolean> positionLocks(String expected, String attempt) {
		String exp = expected == null ? "" : expected;
		String att = attempt == null ? "" : attempt;
		List<Boolean> locks = new ArrayList<>(exp.length());
		for (int i = 0; i < exp.length(); i++) {
			char expCh = exp.charAt(i);
			if (Character.isWhitespace(expCh)) {
				locks.add(Boolean.TRUE);
				continue;
			}
			if (i >= att.length()) {
				locks.add(Boolean.FALSE);
				continue;
			}
			char attCh = att.charAt(i);
			if (attCh == EMPTY_SLOT) {
				locks.add(Boolean.FALSE);
				continue;
			}
			locks.add(Character.toLowerCase(expCh) == Character.toLowerCase(attCh));
		}
		return List.copyOf(locks);
	}

	/**
	 * Score sur les cases lettres seulement (les espaces ne comptent pas).
	 * Ex. 10 lettres justes sur 11 → 91 %.
	 */
	public static int scorePercent(String expected, String attempt) {
		return score(expected, attempt).percent();
	}

	public static Score score(String expected, String attempt) {
		String exp = expected == null ? "" : expected;
		List<Boolean> locks = positionLocks(exp, attempt);
		int total = 0;
		int ok = 0;
		for (int i = 0; i < exp.length(); i++) {
			if (Character.isWhitespace(exp.charAt(i))) {
				continue;
			}
			total++;
			if (Boolean.TRUE.equals(locks.get(i))) {
				ok++;
			}
		}
		return new Score(ok, total);
	}

	public record Score(int correct, int total) {
		public boolean passed() {
			return total > 0 && correct == total;
		}

		public int percent() {
			if (total == 0) {
				return 0;
			}
			return passed() ? 100 : Math.min(99, (int) Math.round((100.0 * correct) / total));
		}
	}

	static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
