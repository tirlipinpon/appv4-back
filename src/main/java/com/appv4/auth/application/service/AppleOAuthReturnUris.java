package com.appv4.auth.application.service;

import java.net.URI;
import java.util.List;

/**
 * Allowlist de l'URL de reprise front après Apple form_post (issue #33).
 */
public final class AppleOAuthReturnUris {

	private AppleOAuthReturnUris() {
	}

	public static boolean isAllowed(String frontendResumeUri, List<String> allowedPrefixes) {
		if (frontendResumeUri == null || frontendResumeUri.isBlank() || allowedPrefixes == null) {
			return false;
		}
		URI uri;
		try {
			uri = URI.create(frontendResumeUri.trim());
		} catch (IllegalArgumentException ex) {
			return false;
		}
		if (uri.getScheme() == null || uri.getHost() == null) {
			return false;
		}
		String normalized = uri.normalize().toString();
		for (String prefix : allowedPrefixes) {
			if (prefix != null && !prefix.isBlank() && normalized.startsWith(prefix.trim())) {
				return true;
			}
		}
		return false;
	}
}
