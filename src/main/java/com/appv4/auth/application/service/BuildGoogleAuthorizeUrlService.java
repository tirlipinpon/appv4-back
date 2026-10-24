package com.appv4.auth.application.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.BuildGoogleAuthorizeUrlUseCase;
import com.appv4.auth.infrastructure.config.GoogleOAuthProperties;

/**
 * Construit l'URL d'autorisation Google (authorization code).
 */
public class BuildGoogleAuthorizeUrlService implements BuildGoogleAuthorizeUrlUseCase {

	private final GoogleOAuthProperties properties;

	public BuildGoogleAuthorizeUrlService(GoogleOAuthProperties properties) {
		this.properties = properties;
	}

	@Override
	public String build(String redirectUri, String state) {
		if (!properties.isConfigured()) {
			throw new AuthDomainException(
					HttpStatus.SERVICE_UNAVAILABLE.value(),
					"Google OAuth non configuré (GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET)");
		}
		if (redirectUri == null || redirectUri.isBlank()) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), "redirectUri obligatoire");
		}
		if (state == null || state.isBlank()) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), "state OAuth obligatoire");
		}
		String scope = URLEncoder.encode("openid email profile", StandardCharsets.UTF_8);
		String encodedRedirect = URLEncoder.encode(redirectUri.trim(), StandardCharsets.UTF_8);
		String clientId = URLEncoder.encode(properties.getClientId(), StandardCharsets.UTF_8);
		String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);
		return properties.getAuthorizationUri()
				+ "?client_id=" + clientId
				+ "&redirect_uri=" + encodedRedirect
				+ "&response_type=code"
				+ "&scope=" + scope
				+ "&state=" + encodedState
				+ "&access_type=online"
				+ "&prompt=select_account";
	}
}
