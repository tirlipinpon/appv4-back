package com.appv4.auth.application.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Encodage {@code state} et contrôle de l'URL de reprise front (Apple form_post, issue #33).
 *
 * <p><b>Business :</b> Apple renvoie le code en POST HTTPS vers le back ; l'utilisateur
 * doit ensuite revenir sur le SPA avec ce code.
 * <p><b>Technique :</b> {@code state} = Base64URL de l'URL de reprise ; allowlist de préfixes.
 * <p><b>Métier / flow :</b> authorize-url → Apple → callback back → redirect FE → échange code.
 */
public final class AppleOAuthReturnUris {

	private AppleOAuthReturnUris() {
	}

	public static String encodeState(String frontendResumeUri) {
		return Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString(frontendResumeUri.getBytes(StandardCharsets.UTF_8));
	}

	static String decodeState(String state) {
		if (state == null || state.isBlank()) {
			throw new IllegalArgumentException("state manquant");
		}
		try {
			return new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("state invalide", ex);
		}
	}

	static boolean isAllowed(String frontendResumeUri, List<String> allowedPrefixes) {
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
