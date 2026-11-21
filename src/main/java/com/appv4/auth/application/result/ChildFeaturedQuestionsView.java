package com.appv4.auth.application.result;

import java.util.List;

/**
 * Questions proposées sur l'accueil de l'enfant (BACK-20).
 *
 * <p><b>Business :</b> l'accueil montre ces énoncés, sans en inventer le titre.
 * <p><b>Technique :</b> seulement l'id de matière, l'id de question et l'énoncé.
 * <p><b>Métier / flow :</b> résultat de {@code GET /api/ext/featured}, avant le JSON.
 */
public record ChildFeaturedQuestionsView(List<FeaturedQuestion> questions) {

	public ChildFeaturedQuestionsView {
		questions = questions == null ? List.of() : List.copyOf(questions);
	}

	public record FeaturedQuestion(long subjectId, long questionId, String prompt) {
	}
}
