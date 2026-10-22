package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port entrant — connexion Facebook (authorization code → JWT).
 *
 * <p><b>Business (SCRUM-99) :</b> 1er login crée le compte ; 2e retrouve le même ;
 * sans email Facebook → refus (pas de compte).
 */
public interface LoginWithFacebookUseCase {

	JwtTokenResult login(String authorizationCode, String redirectUri);
}
