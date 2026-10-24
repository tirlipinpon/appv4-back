package com.appv4.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config Sign in with Apple (SCRUM-102) — secrets via env, jamais committer.
 *
 * <p>{@code APPLE_CLIENT_ID} (Services ID), {@code APPLE_TEAM_ID}, {@code APPLE_KEY_ID},
 * {@code APPLE_PRIVATE_KEY} (PEM .p8, newlines possibles via {@code \n}).
 */
@ConfigurationProperties(prefix = "appv4.oauth.apple")
public class AppleOAuthProperties {

	private String clientId = "";
	private String teamId = "";
	private String keyId = "";
	/** Contenu PEM de la clé .p8 (avec ou sans en-têtes). */
	private String privateKey = "";
	private String authorizationUri = "https://appleid.apple.com/auth/authorize";
	private String tokenUri = "https://appleid.apple.com/auth/token";
	private String jwksUri = "https://appleid.apple.com/auth/keys";

	public boolean isConfigured() {
		return clientId != null && !clientId.isBlank()
				&& teamId != null && !teamId.isBlank()
				&& keyId != null && !keyId.isBlank()
				&& privateKey != null && !privateKey.isBlank();
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
