package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port in — inscription email/mdp (SCRUM-105).
 */
public interface SignupWithEmailUseCase {

	JwtTokenResult signup(String email, String password, String displayName);
}
