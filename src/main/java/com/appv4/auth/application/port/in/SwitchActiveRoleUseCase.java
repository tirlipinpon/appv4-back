package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port entrant — changer d'espace sans se déconnecter (SCRUM-124).
 *
 * <p><b>Business :</b> le front demande parent ou professeur. On n'accepte que
 * si ce rôle est déjà dans {@code back_user_roles}.
 * <p><b>Technique :</b> met à jour {@code last_active_role} puis réémet un JWT.
 * <p><b>Métier / flow :</b> appelé par {@code POST /api/int/auth/switch-role},
 * après le filtre JWT.
 */
public interface SwitchActiveRoleUseCase {

	/**
	 * @param role rôle demandé par le front ({@code parent} ou {@code teacher})
	 * @throws com.appv4.auth.application.exception.AuthDomainException 403 si le compte ne l'a pas
	 */
	JwtTokenResult switchRole(long userId, String email, String role);
}
