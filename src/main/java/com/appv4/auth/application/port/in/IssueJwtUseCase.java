package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port <b>entrant</b> (hexagonal) : cas d'usage « donner un JWT à un user authentifié ».
 *
 * <p><b>Business :</b> le jeton dit dans quel espace revenir après reconnexion.
 * <p><b>Technique :</b> claim {@code activeRole} calculé depuis les rôles et
 * {@code last_active_role}.
 * <p><b>Métier / flow :</b> appelé après login email, login social et inscription.
 */
public interface IssueJwtUseCase {

	/**
	 * @param userId id en base ({@code back_users.id})
	 * @param email  email du compte (claim dans le JWT pour le front)
	 */
	JwtTokenResult issue(long userId, String email);
}
