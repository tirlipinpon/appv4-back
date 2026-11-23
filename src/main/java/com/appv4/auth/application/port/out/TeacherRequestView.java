package com.appv4.auth.application.port.out;

import java.time.Instant;

/**
 * Une demande professeur telle que l'admin la voit (SCRUM-142).
 *
 * @param email adresse du demandeur, pour la file et l'email d'acceptation
 * @param displayName nom choisi à l'inscription, affiché à côté de l'email
 */
public record TeacherRequestView(
		long id,
		long userId,
		String email,
		String displayName,
		String status,
		Instant createdAt,
		Instant reviewedAt,
		Instant revokedAt) {
}
