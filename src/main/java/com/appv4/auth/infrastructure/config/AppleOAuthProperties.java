package com.appv4.auth.infrastructure.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config Sign in with Apple (SCRUM-102) — secrets via env, jamais committer.
 *
 * <p>{@code APPLE_CLIENT_ID} (Services ID), {@code APPLE_TEAM_ID}, {@code APPLE_KEY_ID},
 * {@code APPLE_PRIVATE_KEY} (PEM .p8, newlines possibles via {@code \n}).
 * <p>{@code APPLE_CALLBACK_URI} : URL HTTPS du back qui reçoit le {@code form_post} Apple
 * (issue #33) — enregistrée comme Return URL dans le Services ID.
 */
@ConfigurationProperties(prefix = "appv4.oauth.apple")
public class AppleOAuthProperties {

	private String clientId = "";
	private String teamId = "";
	private String keyId = "";
	/** Contenu PEM de la clé .p8 (avec ou sans en-têtes). */
	private String privateKey = "";
	/**
	 * Redirect URI côté back (form_post). Ex. :
	 * {@code https://api.example.com/api/int/auth/oauth/apple/callback}.
	 */
	private String callbackUri = "";
	/**
	 * Préfixes d'URL SPA autorisés après le callback (anti open-redirect).
	 */
	private List<String> allowedReturnUriPrefixes = new ArrayList<>(List.of(
			"http://localhost:4201/",
			"http://localhost:4200/",
			"https://localhost:4201/",
			"https://localhost:4200/"));
	private String authorizationUri = "https://appleid.apple.com/auth/authorize";
	private String tokenUri = "https://appleid.apple.com/auth/token";
	private String jwksUri = "https://appleid.apple.com/auth/keys";

	public boolean isConfigured() {
		return clientId != null && !clientId.isBlank()
				&& teamId != null && !teamId.isBlank()
				&& keyId != null && !keyId.isBlank()
				&& privateKey != null && !privateKey.isBlank();
	}

	public boolean isCallbackConfigured() {
		return callbackUri != null && !callbackUri.isBlank();
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getTeamId() {
		return teamId;
	}

	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public String getCallbackUri() {
		return callbackUri;
	}

	public void setCallbackUri(String callbackUri) {
		this.callbackUri = callbackUri;
	}

	public List<String> getAllowedReturnUriPrefixes() {
		return allowedReturnUriPrefixes;
	}

	public void setAllowedReturnUriPrefixes(List<String> allowedReturnUriPrefixes) {
		this.allowedReturnUriPrefixes = allowedReturnUriPrefixes != null
				? allowedReturnUriPrefixes
				: new ArrayList<>();
	}

	public String getAuthorizationUri() {
		return authorizationUri;
	}

	public void setAuthorizationUri(String authorizationUri) {
		this.authorizationUri = authorizationUri;
	}

	public String getTokenUri() {
		return tokenUri;
	}

	public void setTokenUri(String tokenUri) {
		this.tokenUri = tokenUri;
	}

	public String getJwksUri() {
		return jwksUri;
	}

	public void setJwksUri(String jwksUri) {
		this.jwksUri = jwksUri;
	}
}
