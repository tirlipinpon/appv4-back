package com.appv4.auth.application.port.out;

import java.time.Instant;
import java.util.List;
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

	List<TeacherRequestView> findPendingAndApproved();

	Optional<TeacherRequestView> findById(long requestId);

	void markApproved(long requestId, Instant reviewedAt);

	void markRevoked(long requestId, Instant reviewedAt);

	/** Ajoute {@code teacher} s'il n'est pas déjà là. */
	void grantTeacher(long userId);

	/** Retire {@code teacher}. Le rôle parent, s'il existe, reste. */
	void revokeTeacher(long userId);
}
