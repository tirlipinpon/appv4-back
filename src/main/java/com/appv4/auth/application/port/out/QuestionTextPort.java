package com.appv4.auth.application.port.out;

import java.util.Collection;
import java.util.Map;

/**
 * Texte actuel des questions encore là.
 *
 * <p><b>Business :</b> le parent voit le texte d'aujourd'hui, pas une copie du moment de l'essai.
 * <p><b>Technique :</b> {@code back_subject_questions.prompt}. Une question absente n'a pas de ligne.
 * <p><b>Métier / flow :</b> lecture parent du suivi, pour les tâches de la mission.
 */
public interface QuestionTextPort {

	Map<Long, String> promptsOf(Collection<Long> questionIds);
}
