package com.appv4.auth.application.port.in;

import java.util.List;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port entrant — choisir Parent ou Professeur après la création du compte (SCRUM-153).
 *
 * <p><b>Business :</b> email, Google et Facebook posent la même question, une seule fois.
 * <p><b>Technique :</b> parent dans {@code back_user_roles}, teacher seulement en demande.
 * <p><b>Métier / flow :</b> appelé par {@code POST /api/int/auth/complete-roles}, jeton déjà émis.
 */
public interface CompleteRolesUseCase {

	/**
	 * @param roles {@code parent} et/ou {@code teacher}
	 */
	JwtTokenResult complete(long userId, String email, List<String> roles);
}
