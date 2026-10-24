package com.appv4.auth.application.config;

/**
 * Valeurs OAuth Google utiles au cœur (issue #31) — pas de binding Spring ici.
 */
public record GoogleOAuthSettings(String clientId, String authorizationUri, boolean secretsConfigured) {

	public boolean isConfigured() {
		return secretsConfigured && clientId != null && !clientId.isBlank();
	}
}
