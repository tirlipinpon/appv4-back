package com.appv4.auth.application.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.BuildAppleAuthorizeUrlUseCase;
import com.appv4.auth.infrastructure.config.AppleOAuthProperties;

/**
 * Construit l'URL Sign in with Apple (response_type=code).
 */
public class BuildAppleAuthorizeUrlService implements BuildAppleAuthorizeUrlUseCase {

	private final AppleOAuthProperties properties;

	public BuildAppleAuthorizeUrlService(AppleOAuthProperties properties) {
		this.properties = properties;
	}

	@Override
	public String build(String redirectUri) {
		if (!properties.isConfigured()) {
			throw new AuthDomainException(
					HttpStatus.SERVICE_UNAVAILABLE.value(),
					"Apple OAuth non configuré (APPLE_CLIENT_ID / TEAM_ID / KEY_ID / PRIVATE_KEY)");
		}
		if (redirectUri == null || redirectUri.isBlank()) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), "redirectUri obligatoire");
		}
		String scope = URLEncoder.encode("name email", StandardCharsets.UTF_8);
		String encodedRedirect = URLEncoder.encode(redirectUri.trim(), StandardCharsets.UTF_8);
		String clientId = URLEncoder.encode(properties.getClientId(), StandardCharsets.UTF_8);
		return properties.getAuthorizationUri()
				+ "?client_id=" + clientId
				+ "&redirect_uri=" + encodedRedirect
				+ "&response_type=code"
				+ "&response_mode=query"
				+ "&scope=" + scope;
	}
}
