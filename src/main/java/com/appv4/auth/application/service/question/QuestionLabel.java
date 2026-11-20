package com.appv4.auth.application.service.question;

import java.text.Normalizer;
import java.util.regex.Pattern;

/** Forme comparable des libellés affichés sur les cartes et les jetons. */
public final class QuestionLabel {
	private static final Pattern SPACE = Pattern.compile("[\\p{Zs}\\t\\r\\n\\f]+");

	private QuestionLabel() {
	}

	public static String comparable(String label) {
		return SPACE.matcher(Normalizer.normalize(label, Normalizer.Form.NFC)).replaceAll(" ").trim();
	}
}
