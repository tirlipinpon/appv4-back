package com.appv4.auth.application.service;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ChildPort.InterestCopy;

/**
 * Règles d'une liste de centres d'intérêt envoyée avec la fiche.
 *
 * <p><b>Business :</b> le parent envoie les mots, on garde son orthographe,
 * et deux écritures de la même forme comparée sont un seul centre d'intérêt.
 * <p><b>Technique :</b> réduit, puis longueur, puis caractères, puis le maximum,
 * puis les doublons. La forme comparée ne s'enregistre pas.
 * <p><b>Métier / flow :</b> appelée par l'ajout et la correction de fiche, avant toute écriture.
 */
public final class ChildInterestList {

	public static final String MISSING = "La liste des centres d'intérêt est manquante.";
	public static final String LENGTH = "Un centre d'intérêt doit faire entre 2 et 40 caractères.";
	public static final String CHARSET =
			"Un centre d'intérêt ne peut contenir que des lettres, des chiffres, des espaces, un tiret ou une apostrophe.";
	public static final String MAXIMUM = "Dix centres d'intérêt maximum.";
	public static final String DUPLICATE = "Ce centre d'intérêt est déjà sur cet enfant.";

	private static final int MINIMUM_LENGTH = 2;
	private static final int MAXIMUM_LENGTH = 40;
	private static final int MAXIMUM_COUNT = 10;

	private ChildInterestList() {
	}

	/**
	 * Accepte la liste, ou refuse sans produire de copies.
	 *
	 * <p><b>Business :</b> une liste vide efface. Une liste absente est refusée.
	 * Réécrire la même forme garde le moment et prend la nouvelle orthographe.
	 * <p><b>Technique :</b> {@code raw} null lève l'absence. Les mots nouveaux d'un même appel
	 * reçoivent {@code now}.
	 * <p><b>Métier / flow :</b> avant l'écriture de la fiche. Si cette méthode refuse,
	 * l'appelant n'écrit ni la fiche ni les copies.
	 */
	public static List<InterestCopy> resolve(List<String> raw, List<InterestCopy> previous, Instant now) {
		if (raw == null) {
			throw bad(MISSING);
		}
		List<String> reduced = new ArrayList<>();
		for (String word : raw) {
			String text = reduce(word);
			int length = text.codePointCount(0, text.length());
			if (length < MINIMUM_LENGTH || length > MAXIMUM_LENGTH) {
				throw bad(LENGTH);
			}
			if (!allowed(text)) {
				throw bad(CHARSET);
			}
			reduced.add(text);
		}
		if (reduced.size() > MAXIMUM_COUNT) {
			throw bad(MAXIMUM);
		}
		List<String> forms = new ArrayList<>();
		for (String text : reduced) {
			String form = form(text);
			if (forms.contains(form)) {
				throw bad(DUPLICATE);
			}
			forms.add(form);
		}
		List<InterestCopy> next = new ArrayList<>();
		for (int index = 0; index < reduced.size(); index++) {
			InterestCopy kept = previousOf(previous, forms.get(index));
			if (kept == null) {
				next.add(new InterestCopy(reduced.get(index), now));
			} else {
				next.add(new InterestCopy(reduced.get(index), kept.writtenAt()));
			}
		}
		return List.copyOf(next);
	}

	/**
	 * Orthographes à montrer sur la fiche.
	 *
	 * <p><b>Business :</b> le parent revoit ses mots, du premier au dernier selon la forme comparée.
	 * <p><b>Technique :</b> le tri suit les caractères de la forme, pas l'ordre d'envoi.
	 * <p><b>Métier / flow :</b> réponse de l'ajout, de la correction et de la lecture.
	 */
	public static List<String> spellings(List<InterestCopy> copies) {
		List<InterestCopy> ordered = new ArrayList<>(copies);
		ordered.sort((left, right) -> compareForms(form(left.text()), form(right.text())));
		List<String> texts = new ArrayList<>();
		for (InterestCopy copy : ordered) {
			texts.add(copy.text());
		}
		return List.copyOf(texts);
	}

	private static InterestCopy previousOf(List<InterestCopy> previous, String form) {
		for (InterestCopy copy : previous) {
			if (form(copy.text()).equals(form)) {
				return copy;
			}
		}
		return null;
	}

	static String reduce(String word) {
		if (word == null) {
			return "";
		}
		String gap = "[\\t\\n\\r\\u2028\\u2029 ]";
		String trimmed = word.replaceAll("^" + gap + "+|" + gap + "+$", "");
		return trimmed.replaceAll(gap + "+", " ");
	}

	private static boolean allowed(String text) {
		int index = 0;
		while (index < text.length()) {
			int codePoint = text.codePointAt(index);
			boolean letter = Character.isLetter(codePoint) || Character.isDigit(codePoint);
			boolean sign = codePoint == ' ' || codePoint == '-' || codePoint == '\'' || codePoint == '\u2019';
			if (!letter && !sign) {
				return false;
			}
			index += Character.charCount(codePoint);
		}
		return true;
	}

	static String form(String text) {
		String lower = text.toLowerCase(Locale.ROOT).replace("œ", "oe").replace("æ", "ae");
		String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
		StringBuilder folded = new StringBuilder(decomposed.length());
		int index = 0;
		while (index < decomposed.length()) {
			int codePoint = decomposed.codePointAt(index);
			int type = Character.getType(codePoint);
			boolean mark = type == Character.NON_SPACING_MARK
					|| type == Character.ENCLOSING_MARK
					|| type == Character.COMBINING_SPACING_MARK;
			if (!mark) {
				folded.appendCodePoint(codePoint == '\u2019' ? '\'' : codePoint);
			}
			index += Character.charCount(codePoint);
		}
		return folded.toString();
	}

	static int compareForms(String left, String right) {
		int leftIndex = 0;
		int rightIndex = 0;
		while (leftIndex < left.length() && rightIndex < right.length()) {
			int leftCode = left.codePointAt(leftIndex);
			int rightCode = right.codePointAt(rightIndex);
			if (leftCode != rightCode) {
				return Integer.compare(leftCode, rightCode);
			}
			leftIndex += Character.charCount(leftCode);
			rightIndex += Character.charCount(rightCode);
		}
		return Integer.compare(left.length() - leftIndex, right.length() - rightIndex);
	}

	private static AuthDomainException bad(String message) {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, message);
	}
}
