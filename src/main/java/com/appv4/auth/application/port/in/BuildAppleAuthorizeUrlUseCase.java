package com.appv4.auth.application.port.in;

/**
 * Port in — URL d'autorisation Apple (Services ID + form_post vers le callback back).
 *
 * @param frontendResumeUri URL SPA de reprise après le callback (ex. {@code .../auth/callback/apple})
 * @param state             liaison navigateur (issue #32)
 */
public interface BuildAppleAuthorizeUrlUseCase {

	String build(String frontendResumeUri, String state);
}
