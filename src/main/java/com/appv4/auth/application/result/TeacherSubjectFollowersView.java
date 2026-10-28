package com.appv4.auth.application.result;

import java.util.List;

/**
 * Enfants qui suivent encore une matière, au moment où le professeur veut en retirer des questions.
 *
 * <p><b>Business :</b> avant de supprimer, le professeur voit combien d'enfants suivent
 * ce cours et leurs noms. Un enfant de la classe compte, sauf si le parent a retiré
 * le cours. Un enfant d'une autre classe compte s'il a été ajouté à ce cours.
 * <p><b>Technique :</b> {@code warning} est la phrase à afficher. {@code names} est
 * « prénom nom », tri français. {@code childCount} est le nombre de noms.
 * <p><b>Métier / flow :</b> résultat de {@code GET /api/int/teacher/subjects/{id}/followers}.
 */
public record TeacherSubjectFollowersView(int childCount, String warning, List<String> names) {

	public TeacherSubjectFollowersView {
		names = List.copyOf(names);
	}
}
