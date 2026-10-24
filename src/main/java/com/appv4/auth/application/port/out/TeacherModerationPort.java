package com.appv4.auth.application.port.out;

import java.time.Instant;
import java.util.Optional;

/**
 * Port sortant — file des demandes professeur (SCRUM-142).
 *
 * <p><b>Business :</b> l'admin voit l'attente et ce qu'il a déjà accepté.
 * Accepter donne le rôle teacher, une seule fois.
 * <p><b>Technique :</b> {@code back_teacher_role_requests} et {@code back_user_roles}.
 * <p><b>Métier / flow :</b> appelé par la modération, après le contrôle du rôle admin.
 */
public interface TeacherModerationPort {

	/**
	 * File pending/approved/revoked, ordre créé puis id, au plus {@code limit} lignes.
	 */
	TeacherRequestPage findModerationQueue(int limit);

	Optional<TeacherRequestView> findById(long requestId);

	/**
	 * Passe à approved de façon atomique (issue #46).
	 *
	 * <p>Un seul concurrent depuis pending/revoked obtient {@link TeacherApprovalOutcome#FIRST_APPROVAL}
	 * (à notifier). Les autres / les réapprobations obtiennent {@link TeacherApprovalOutcome#REVALIDATED}.
	 */
	TeacherApprovalOutcome markApproved(long requestId, Instant reviewedAt);

	/** Passe à revoked et pose la date du retrait. La date du oui reste. */
	void markRevoked(long requestId, Instant revokedAt);

	/** Ajoute {@code teacher} s'il n'est pas déjà là. */
	void grantTeacher(long userId);

	/** Retire {@code teacher}. Le rôle parent, s'il existe, reste. */
	void revokeTeacher(long userId);
}
