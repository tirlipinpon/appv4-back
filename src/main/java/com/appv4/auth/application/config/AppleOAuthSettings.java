package com.appv4.auth.application.config;

import java.util.List;

/**
 * Valeurs OAuth Apple utiles au cœur (issue #31) — secrets PEM restent en infra.
 */
public record AppleOAuthSettings(
		String clientId,
		String authorizationUri,
		String callbackUri,
		List<String> allowedReturnUriPrefixes,
		boolean secretsConfigured) {

	public AppleOAuthSettings {
		allowedReturnUriPrefixes = allowedReturnUriPrefixes == null
				? List.of()
				: List.copyOf(allowedReturnUriPrefixes);
	}

	public boolean isConfigured() {
		return secretsConfigured && clientId != null && !clientId.isBlank();
	}

	public boolean isCallbackConfigured() {
		return callbackUri != null && !callbackUri.isBlank();
	}
}
