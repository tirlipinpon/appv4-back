package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.PIECE_BOTH;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

/**
 * Une face est un texte ou une image, jamais les deux.
 *
 * <p><b>Business :</b> le professeur choisit l'un ou l'autre. Une ligne vide est ignorée
 * (ligne encore vierge du formulaire).
 * <p><b>Technique :</b> l'image arrive en WebP base64, déjà réduite par l'écran.
 * <p><b>Métier / flow :</b> appelée par les formes à pièces avant l'enregistrement.
 */
final class PieceFaces {

	private PieceFaces() {
	}

	record Face(String label, byte[] imageWebp) {
	}

	/** {@code null} si la ligne n'a ni texte ni image. */
	static Face optional(String label, String imageRaw, int max, String tooLong) {
		byte[] image = imageRaw == null || imageRaw.isBlank() ? null : DrawnImageRules.requireWebp(imageRaw);
		String text = label == null ? "" : label.trim();
		if (image != null && !text.isEmpty()) {
			throw bad(PIECE_BOTH);
		}
		if (image == null && text.isEmpty()) {
			return null;
		}
		if (text.length() > max) {
			throw bad(tooLong);
		}
		return new Face(image == null ? text : null, image);
	}
}
