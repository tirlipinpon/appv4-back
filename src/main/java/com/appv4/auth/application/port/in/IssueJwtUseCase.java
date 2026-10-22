package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port <b>entrant</b> (hexagonal) : cas d'usage « donner un JWT à un user authentifié ».
 *
 * <p>Aujourd'hui surtout utilisé par les tests. Demain : appelé après login email/mdp
 * ou après callback Google/Facebook/Apple.
 */
public interface IssueJwtUseCase {

	/**
	 * @param userId id en base ({@code back_users.id})
	 * @param email  email du compte (claim dans le JWT pour le front)
	 */
	JwtTokenResult issue(long userId, String email);
}
