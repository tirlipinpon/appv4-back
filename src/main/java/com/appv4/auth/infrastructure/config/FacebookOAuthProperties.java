package com.appv4.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config Facebook Login (SCRUM-99) — secrets via env, jamais committer.
 *
 * <p>{@code FACEBOOK_APP_ID} / {@code FACEBOOK_APP_SECRET} (Meta for Developers).
 */
@ConfigurationProperties(prefix = "appv4.oauth.facebook")
public class FacebookOAuthProperties {

	private String appId = "";
	private String appSecret = "";
	private String authorizationUri = "https://www.facebook.com/v21.0/dialog/oauth";
	private String tokenUri = "https://graph.facebook.com/v21.0/oauth/access_token";
	private String userInfoUri = "https://graph.facebook.com/v21.0/me";

	public boolean isConfigured() {
		return appId != null && !appId.isBlank()
				&& appSecret != null && !appSecret.isBlank();
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getAppSecret() {
		return appSecret;
	}

	public void setAppSecret(String appSecret) {
		this.appSecret = appSecret;
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
