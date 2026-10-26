package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.TeacherSubjectsView;
import com.appv4.auth.application.result.TeacherSubjectsView.SubjectLine;

/**
 * Créer, revoir, renommer, retirer et transférer les matières de ses classes
 * (SCRUM-161, SCRUM-172).
 *
 * <p><b>Business :</b> le professeur ne pose une matière que sur une année déjà
 * cochée. Il peut la déplacer ou la partager avec un collègue accepté qui a la
 * même classe. Les formes de questions restent pour une story suivante.
 * <p><b>Technique :</b> l'identifiant vient du jeton, jamais du corps de la requête.
 * <p><b>Métier / flow :</b> port in de {@code /api/int/teacher/subjects}, après le
 * filtre JWT, avant le port matières et le profil (écoles et années).
 */
public interface TeacherSubjectUseCase {

	TeacherSubjectsView list(long userId, String activeRole);

	SubjectLine create(long userId, String activeRole, Long schoolId, String grade, String name);

	SubjectLine rename(long userId, String activeRole, long subjectId, String name);

	void deactivate(long userId, String activeRole, long subjectId);

	void reactivate(long userId, String activeRole, long subjectId);

	/** Retire l'accès de ce professeur. La matière reste si d'autres y ont accès. */
	void remove(long userId, String activeRole, long subjectId);

	/**
	 * Déplace ou partage la matière vers un collègue (même id, pas une copie).
	 *
	 * @param mode {@code move} ou {@code share}
	 */
	void transfer(long userId, String activeRole, long subjectId, String email, String mode);
}
