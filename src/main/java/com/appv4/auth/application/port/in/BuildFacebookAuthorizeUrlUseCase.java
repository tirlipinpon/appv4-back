package com.appv4.auth.application.port.in;

/**
 * Construit l'URL Facebook à ouvrir dans le navigateur (démarrage OAuth).
 */
public interface BuildFacebookAuthorizeUrlUseCase {

	String build(String redirectUri);
}
