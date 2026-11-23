package com.appv4.auth.application.port.out;

/**
 * Port out — échange code Apple → profil (sub + email éventuel).
 *
 * <p>L'email n'est fourni qu'au <b>1er consentement</b> ; ensuite on retombe sur
 * {@code provider_subject}.
 */
public interface AppleOAuthClient {

	AppleUserProfile exchangeCode(String authorizationCode, String redirectUri);

	record AppleUserProfile(String subject, String email, String displayName) {
	}
}
