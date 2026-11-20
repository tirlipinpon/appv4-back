package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.ChildLearningQuestionsView;
import com.appv4.auth.application.result.ChildLearningSubjectsView;

/**
 * Port entrant — matières et questions de l'enfant connecté (BACK-15).
 *
 * <p><b>Business :</b> Learn n'affiche que ce qui est lié à la fiche.
 * <p><b>Technique :</b> jeton enfant obligatoire.
 * <p><b>Métier / flow :</b> {@code /api/ext/subjects} → ce port → liens + questions.
 */
public interface ReadChildLearningSubjectsUseCase {

	ChildLearningSubjectsView list(String rawToken);

	ChildLearningQuestionsView questions(String rawToken, long subjectId, long afterId, int limit);
}
