package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.ChildFeaturedQuestionsView;

/**
 * Lecture des questions d'accueil de l'enfant connecté (BACK-20).
 *
 * <p><b>Business :</b> l'accueil affiche cette liste. Il ne choisit pas les questions.
 * <p><b>Technique :</b> {@code GET /api/ext/featured}, jeton enfant.
 * <p><b>Métier / flow :</b> adapter in → ce port → matières, questions et étoiles.
 */
public interface ReadChildFeaturedQuestionsUseCase {

	ChildFeaturedQuestionsView read(String rawToken);
}
