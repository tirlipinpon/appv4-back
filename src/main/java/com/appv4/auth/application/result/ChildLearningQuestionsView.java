package com.appv4.auth.application.result;

import java.util.List;

/**
 * Questions d'une matière pour l'enfant connecté (BACK-15).
 *
 * <p><b>Business :</b> lecture seule : id, forme et énoncé. Pas de correction ici.
 */
public record ChildLearningQuestionsView(long subjectId, String subjectName, List<QuestionCard> questions) {

	public ChildLearningQuestionsView {
		questions = questions == null ? List.of() : List.copyOf(questions);
	}

	public record QuestionCard(long questionId, String formCode, String prompt) {
	}
}
