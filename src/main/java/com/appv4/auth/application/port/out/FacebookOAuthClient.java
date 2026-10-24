package com.appv4.auth.application.port.out;

/**
 * Port sortant — parler à Facebook (échange code → profil).
 *
 * <p><b>Business :</b> même règle que Google : 1 email = 1 compte.
 * <p><b>Technique :</b> authorization code → Graph access_token → {@code /me}.
 */
public interface FacebookOAuthClient {

	FacebookUserProfile exchangeCode(String authorizationCode, String redirectUri);

	/**
	 * @param subject     id Facebook stable ({@code id})
	 * @param email       peut être null si l'utilisateur refuse le scope email
	 * @param displayName nom affiché
	 */
	record FacebookUserProfile(String subject, String email, String displayName) {
	}
}
