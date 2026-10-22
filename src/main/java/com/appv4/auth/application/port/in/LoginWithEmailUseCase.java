package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port entrant — connexion email/mdp (SCRUM-117).
 *
 * <p><b>Business :</b> entrer dans l'admin avec le compte perso (pas OAuth).
 * <p><b>Technique :</b> vérifie email + BCrypt ; échec → message unique 401.
 * <p><b>Métier / flow :</b> controller → ce port → AuthUserAccountPort + IssueJwtUseCase.
 */
public interface LoginWithEmailUseCase {

	JwtTokenResult login(String email, String password);
}
