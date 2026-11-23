package com.appv4.auth.application.result;

import java.util.List;

/**
 * Alertes matières reçues par le professeur connecté.
 *
 * <p><b>Business :</b> désactivation, confirmation ou refus de reprise, etc.
 * <p><b>Technique :</b> message déjà formé côté back ; {@code kind} pour l'UI.
 * <p><b>Métier / flow :</b> {@code GET /api/int/teacher/subjects/deactivate-notices}.
 */
public record TeacherDeactivateNoticesView(List<NoticeLine> notices) {

	public TeacherDeactivateNoticesView {
		notices = notices == null ? List.of() : List.copyOf(notices);
	}

	/**
	 * @param kind    {@code deactivate}, {@code handover_accepted}, {@code handover_rejected}
	 * @param message texte prêt à afficher
	 */
	public record NoticeLine(long id, long subjectId, String subjectName, String kind, String message) {
	}
}
