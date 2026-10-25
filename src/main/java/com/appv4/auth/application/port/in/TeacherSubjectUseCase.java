package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.TeacherSubjectsView;
import com.appv4.auth.application.result.TeacherSubjectsView.SubjectLine;

/**
 * Créer, revoir, renommer et retirer les matières de ses classes (SCRUM-161).
 *
 * <p><b>Business :</b> le professeur ne pose une matière que sur une année déjà
 * cochée. Le clonage vers un collègue, l'échange, et les formes de questions
 * (plusieurs formes pour Mathématiques) restent pour une story suivante.
 * <p><b>Technique :</b> l'identifiant vient du jeton, jamais du corps de la requête.
 * <p><b>Métier / flow :</b> port in de {@code /api/int/teacher/subjects}, après le
 * filtre JWT, avant le port matières et le profil (écoles et années).
 */
public interface TeacherSubjectUseCase {

	TeacherSubjectsView list(long userId, String activeRole);

	SubjectLine create(long userId, String activeRole, Long schoolId, String grade, String name);

	SubjectLine rename(long userId, String activeRole, long subjectId, String name);

	void remove(long userId, String activeRole, long subjectId);
}
