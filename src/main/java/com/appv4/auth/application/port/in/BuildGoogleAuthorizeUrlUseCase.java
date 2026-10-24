package com.appv4.auth.application.port.in;

/**
 * Construit l'URL Google à ouvrir dans le navigateur (démarrage OAuth).
 * Le {@code client_id} n'est pas secret ; le secret reste côté back au callback.
 */
public interface BuildGoogleAuthorizeUrlUseCase {

	/**
	 * @param redirectUri URI de callback FE
	 * @param state       liaison navigateur (issue #32)
	 */
	String build(String redirectUri, String state);
}
