package com.appv4.auth.application.port.out;

import java.util.Optional;

/**
 * Lecture de l'énoncé et du nom de matière d'une question (BACK-79).
 *
 * <p><b>Business :</b> le séjour a besoin du texte joué et du nom de la matière.
 * <p><b>Technique :</b> une lecture par identifiant. Vide si la question n'existe plus.
 * <p><b>Métier / flow :</b> port out du cas d'usage Séjour, à côté de {@link SejourPort}.
 */
public interface SejourQuestionPort {

	Optional<QuestionCaption> find(long questionId);

	/**
	 * Texte actuel de la question.
	 *
	 * @param prompt énoncé
	 * @param subjectName nom de la matière qui porte la question
	 */
	record QuestionCaption(String prompt, String subjectName) {
	}
}
