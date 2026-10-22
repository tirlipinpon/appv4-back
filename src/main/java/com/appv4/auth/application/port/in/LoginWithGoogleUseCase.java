package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port entrant — connexion Google (authorization code → JWT).
 *
 * <p><b>Métier (SCRUM-96) :</b> 1er login crée le compte ; 2e login retrouve le même ;
 * email Google non vérifié → refus.
 */
public interface LoginWithGoogleUseCase {

	JwtTokenResult login(String authorizationCode, String redirectUri);
}
