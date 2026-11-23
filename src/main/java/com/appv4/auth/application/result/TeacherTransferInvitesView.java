package com.appv4.auth.application.result;

import java.util.List;

/**
 * Demandes de transfert reçues par le professeur connecté (SCRUM-189).
 *
 * <p><b>Business :</b> chaque ligne décrit ce que le collègue propose
 * (déplacer ou partager) avant que l'accès change.
 * <p><b>Technique :</b> noms et libellés viennent du back, pas du front.
 * <p><b>Métier / flow :</b> {@code GET /api/int/teacher/subjects/transfer-invites}.
 */
public record TeacherTransferInvitesView(List<InviteLine> invites) {

	public TeacherTransferInvitesView {
		invites = invites == null ? List.of() : List.copyOf(invites);
	}

	/**
	 * @param mode    {@code move}, {@code share}, {@code ask} ou {@code handover}
	 * @param message texte prêt à afficher (source de vérité back)
	 */
	public record InviteLine(
			long id,
			long subjectId,
			String subjectName,
			long schoolId,
			String schoolName,
			String grade,
			String mode,
			String fromDisplayName,
			String message) {
	}
}
