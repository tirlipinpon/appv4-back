package com.appv4.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config Google OAuth (SCRUM-96) — secrets via env, jamais committer.
 *
 * <p>{@code GOOGLE_CLIENT_ID} / {@code GOOGLE_CLIENT_SECRET} (console Google Cloud).
 */
@ConfigurationProperties(prefix = "appv4.oauth.google")
public class GoogleOAuthProperties {

	private String clientId = "";
	private String clientSecret = "";
	private String authorizationUri = "https://accounts.google.com/o/oauth2/v2/auth";
	private String tokenUri = "https://oauth2.googleapis.com/token";
	private String userInfoUri = "https://openidconnect.googleapis.com/v1/userinfo";

	public boolean isConfigured() {
		return clientId != null && !clientId.isBlank()
				&& clientSecret != null && !clientSecret.isBlank();
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
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

	public String getUserInfoUri() {
		return userInfoUri;
	}

	public void setUserInfoUri(String userInfoUri) {
		this.userInfoUri = userInfoUri;
	}
}
