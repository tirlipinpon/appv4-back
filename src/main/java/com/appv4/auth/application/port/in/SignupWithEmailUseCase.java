package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port in — inscription email/mdp (SCRUM-105) avec choix de rôle (SCRUM-121).
 *
 * <p><b>Business :</b> créer le compte email/mot de passe. Le choix Parent ou
 * Professeur se fait ensuite, le même appel que pour Google et Facebook.
 */
public interface SignupWithEmailUseCase {

	JwtTokenResult signup(String email, String password, String displayName);
}
