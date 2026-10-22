package com.appv4.auth.application.port.in;

import java.util.List;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port in — inscription email/mdp (SCRUM-105) avec choix de rôle (SCRUM-121).
 *
 * <p><b>Business :</b> créer le compte, donner parent tout de suite, et
 * n'enregistrer professeur que comme une demande en attente.
 */
public interface SignupWithEmailUseCase {

	JwtTokenResult signup(String email, String password, String displayName, List<String> roles);
}
