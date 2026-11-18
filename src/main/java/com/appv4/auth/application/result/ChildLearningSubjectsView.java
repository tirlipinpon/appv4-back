package com.appv4.auth.application.result;

import java.util.List;

/**
 * Matières jouables de l'enfant connecté (BACK-15 / LEARN-29).
 *
 * <p><b>Business :</b> seulement les matières encore liées et actives.
 * Le compteur de questions vient du back ; l'icône aussi.
 */
public record ChildLearningSubjectsView(List<SubjectCard> subjects) {

	public ChildLearningSubjectsView {
		subjects = subjects == null ? List.of() : List.copyOf(subjects);
	}

	/**
	 * @param questionCount total des questions de la matière
	 * @param iconCode      code librairie, ou null
	 */
	public record SubjectCard(
			long subjectId,
			String name,
			String grade,
			long questionCount,
			String iconCode) {
	}
}
