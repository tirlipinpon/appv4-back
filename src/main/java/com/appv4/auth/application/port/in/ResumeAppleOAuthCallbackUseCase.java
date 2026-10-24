package com.appv4.auth.application.port.in;

import java.net.URI;

/**
 * Port in — relais form_post Apple vers le SPA (issue #33).
 */
public interface ResumeAppleOAuthCallbackUseCase {

	/**
	 * @param state state émis à l'authorize (reprise FE encodée)
	 * @param code  code Apple, ou null si erreur
	 * @param error code d'erreur Apple, ou null si succès
	 * @return URI de redirection navigateur (302)
	 */
	URI resume(String state, String code, String error);
}
