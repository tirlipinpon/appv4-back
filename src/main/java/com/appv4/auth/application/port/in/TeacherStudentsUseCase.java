package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.TeacherStudentsView;

/**
 * Élèves des classes du professeur connecté (SCRUM-160).
 *
 * <p><b>Business :</b> il voit qui est inscrit dans une école et une année qu'il a cochées.
 * Il ne modifie personne. Le code PIN et le parent ne sont pas dans cette lecture.
 * <p><b>Technique :</b> le rôle actif du jeton doit être {@code teacher}.
 * <p><b>Métier / flow :</b> port in de {@code /api/int/teacher/students}.
 */
public interface TeacherStudentsUseCase {

	TeacherStudentsView list(long userId, String activeRole);

	/**
	 * Photo d'un élève qui est dans une classe de ce professeur.
	 * Un autre enfant, ou une fiche sans photo, n'a pas d'image ici.
	 */
	StudentPhoto photo(long userId, String activeRole, long childId);

	record StudentPhoto(byte[] bytes, String contentType) {
	}
}
