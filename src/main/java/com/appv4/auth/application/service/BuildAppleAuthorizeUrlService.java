package com.appv4.auth.application.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.BuildAppleAuthorizeUrlUseCase;
import com.appv4.auth.infrastructure.config.AppleOAuthProperties;

/**
 * Construit l'URL Sign in with Apple (code + form_post, issue #33).
 *
 * <p><b>Business :</b> avec les scopes {@code name email}, Apple exige {@code form_post}
 * vers une Return URL HTTPS — pas un query vers le SPA.
 * <p><b>Technique :</b> {@code redirect_uri} = callback back ; {@code state} porte l'URL SPA.
 * <p><b>Métier / flow :</b> authorize-url → Apple POST callback → redirect FE → login JSON.
 */
public class BuildAppleAuthorizeUrlService implements BuildAppleAuthorizeUrlUseCase {

	private final AppleOAuthProperties properties;

	public BuildAppleAuthorizeUrlService(AppleOAuthProperties properties) {
		this.properties = properties;
	}

	@Override
	public String build(String frontendResumeUri) {
		if (!properties.isConfigured()) {
			throw new AuthDomainException(
					HttpStatus.SERVICE_UNAVAILABLE.value(),
					"Apple OAuth non configuré (APPLE_CLIENT_ID / TEAM_ID / KEY_ID / PRIVATE_KEY)");
		}
		if (!properties.isCallbackConfigured()) {
			throw new AuthDomainException(
					HttpStatus.SERVICE_UNAVAILABLE.value(),
					"Apple OAuth callback non configuré (APPLE_CALLBACK_URI)");
		}
		if (frontendResumeUri == null || frontendResumeUri.isBlank()) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), "redirectUri obligatoire");
		}
		String resume = frontendResumeUri.trim();
		if (!AppleOAuthReturnUris.isAllowed(resume, properties.getAllowedReturnUriPrefixes())) {
			throw new AuthDomainException(
					HttpStatus.BAD_REQUEST.value(),
					"redirectUri non autorisé pour la reprise Apple");
		}
		String scope = URLEncoder.encode("name email", StandardCharsets.UTF_8);
		String encodedRedirect = URLEncoder.encode(properties.getCallbackUri().trim(), StandardCharsets.UTF_8);
		String clientId = URLEncoder.encode(properties.getClientId(), StandardCharsets.UTF_8);
		String state = URLEncoder.encode(AppleOAuthReturnUris.encodeState(resume), StandardCharsets.UTF_8);
		return properties.getAuthorizationUri()
				+ "?client_id=" + clientId
				+ "&redirect_uri=" + encodedRedirect
				+ "&response_type=code"
				+ "&response_mode=form_post"
				+ "&scope=" + scope
				+ "&state=" + state;
	}
}
