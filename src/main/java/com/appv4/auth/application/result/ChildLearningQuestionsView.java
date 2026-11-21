package com.appv4.auth.application.result;

import java.util.List;

/**
 * Questions d'une matière pour l'enfant connecté (BACK-15).
 *
 * <p><b>Business :</b> lecture seule : id, forme, énoncé, et l'étoile déjà enregistrée pour cet enfant.
 */
public record ChildLearningQuestionsView(long subjectId, String subjectName, List<QuestionCard> questions,
		Long nextCursor) {

	public ChildLearningQuestionsView {
		questions = questions == null ? List.of() : List.copyOf(questions);
	}

	/**
	 * @param formLabel libellé de la forme, celui du catalogue
	 * @param iconCode  dessin de la forme, ou null
	 * @param color       teinte #rrggbb de la forme, ou null
	 * @param mark        étoile de l'enfant : {@code none}, {@code black} ou {@code yellow}
	 * @param yellowCount nombre d'étoiles jaunes, zéro s'il n'y en a pas
	 */
	public record QuestionCard(
			long questionId,
			String formCode,
			String prompt,
			String formLabel,
			String iconCode,
			String color,
			String mark,
			int yellowCount) {
	}
}
