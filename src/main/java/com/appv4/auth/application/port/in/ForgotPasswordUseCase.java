package com.appv4.auth.application.port.in;

/**
 * Port in — demande de reset mdp (SCRUM-108).
 */
public interface ForgotPasswordUseCase {

	/**
	 * Toujours le même message public (anti-énumération).
	 */
	String requestReset(String email);
}
