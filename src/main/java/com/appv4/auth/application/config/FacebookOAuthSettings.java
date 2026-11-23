package com.appv4.auth.application.config;

/**
 * Valeurs OAuth Facebook utiles au cœur (issue #31).
 */
public record FacebookOAuthSettings(String appId, String authorizationUri, boolean secretsConfigured) {

	public boolean isConfigured() {
		return secretsConfigured && appId != null && !appId.isBlank();
	}
}
