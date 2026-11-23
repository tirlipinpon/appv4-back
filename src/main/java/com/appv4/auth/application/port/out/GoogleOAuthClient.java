package com.appv4.auth.application.port.out;

/**
 * Port sortant — parler à Google (échange code → profil utilisateur).
 */
public interface GoogleOAuthClient {

	/**
	 * Échange le {@code authorization code} contre un access token, puis lit le profil.
	 *
	 * @param authorizationCode code renvoyé par Google après consentement
	 * @param redirectUri       doit être <b>identique</b> à celui du démarrage OAuth
	 */
	GoogleUserProfile exchangeCode(String authorizationCode, String redirectUri);

	/**
	 * Profil Google après OAuth réussi.
	 *
	 * @param subject       id stable Google ({@code sub})
	 * @param email         email du compte
	 * @param emailVerified Google a-t-il vérifié l'email ?
	 * @param displayName   nom affiché (peut être vide)
	 */
	record GoogleUserProfile(String subject, String email, boolean emailVerified, String displayName) {
	}
}
