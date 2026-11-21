package com.appv4.auth.application.result;

import java.util.List;

import com.appv4.auth.application.port.out.QuestionMark;

/**
 * Leçon d'une matière pour l'enfant connecté.
 *
 * <p><b>Business :</b> les questions encore à clarifier, de la plus ancienne à la plus récente.
 * <p><b>Technique :</b> le compte est la taille de la liste. L'étoile est celle déjà enregistrée.
 * <p><b>Métier / flow :</b> lecture du board matière, sans écrire d'essai ni de badge.
 */
public record ChildLessonView(int remainingCount, List<Question> questions) {

	public ChildLessonView {
		questions = questions == null ? List.of() : List.copyOf(questions);
	}

	public static ChildLessonView of(List<Question> questions) {
		List<Question> copy = questions == null ? List.of() : List.copyOf(questions);
		return new ChildLessonView(copy.size(), copy);
	}

	public record Question(long questionId, String prompt, QuestionMark mark, int yellowCount) {
	}
}
