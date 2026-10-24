package com.appv4.auth.application.port.in;

/**
 * Construit l'URL Facebook à ouvrir dans le navigateur (démarrage OAuth).
 */
public interface BuildFacebookAuthorizeUrlUseCase {

	/**
	 * @param redirectUri URI de callback FE
	 * @param state       liaison navigateur (issue #32)
	 */
	String build(String redirectUri, String state);
}