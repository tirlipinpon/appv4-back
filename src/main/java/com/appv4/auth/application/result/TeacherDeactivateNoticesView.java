package com.appv4.auth.application.result;

import java.util.List;

/**
 * Alertes de désactivation reçues par le professeur connecté.
 *
 * <p><b>Business :</b> le créateur a désactivé la matière pour lui et
 * éventuellement des co-profs ; chaque autre détenteur est prévenu.
 * <p><b>Technique :</b> message déjà formé côté back.
 * <p><b>Métier / flow :</b> {@code GET /api/int/teacher/subjects/deactivate-notices}.
 */
public record TeacherDeactivateNoticesView(List<NoticeLine> notices) {

	public TeacherDeactivateNoticesView {
		notices = notices == null ? List.of() : List.copyOf(notices);
	}

	/**
	 * @param message texte prêt à afficher (qui a désactivé, pour qui)
	 */
	public record NoticeLine(long id, long subjectId, String subjectName, String message) {
	}
}
