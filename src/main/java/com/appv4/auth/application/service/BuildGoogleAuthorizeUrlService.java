package com.appv4.auth.application.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;


import com.appv4.auth.application.config.GoogleOAuthSettings;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.BuildGoogleAuthorizeUrlUseCase;

/**
 * Construit l'URL d'autorisation Google (authorization code).
 */
public class BuildGoogleAuthorizeUrlService implements BuildGoogleAuthorizeUrlUseCase {

	private final Supplier<GoogleOAuthSettings> settings;

	public BuildGoogleAuthorizeUrlService(Supplier<GoogleOAuthSettings> settings) {
		this.settings = settings;
	}

	public BuildGoogleAuthorizeUrlService(GoogleOAuthSettings settings) {
		this(() -> settings);
	}

	@Override
	public String build(String redirectUri, String state) {
		GoogleOAuthSettings s = settings.get();
		if (!s.isConfigured()) {
			throw new AuthDomainException(AuthErrorCode.SERVICE_UNAVAILABLE,
					"Google OAuth non configuré (GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET)");
		}
		if (redirectUri == null || redirectUri.isBlank()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, "redirectUri obligatoire");
		}
		if (state == null || state.isBlank()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, "state OAuth obligatoire");
		}
		String scope = URLEncoder.encode("openid email profile", StandardCharsets.UTF_8);
		String encodedRedirect = URLEncoder.encode(redirectUri.trim(), StandardCharsets.UTF_8);
		String clientId = URLEncoder.encode(s.clientId(), StandardCharsets.UTF_8);
		String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);
		return s.authorizationUri()
				+ "?client_id=" + clientId
				+ "&redirect_uri=" + encodedRedirect
				+ "&response_type=code"
				+ "&scope=" + scope
				+ "&state=" + encodedState
				+ "&access_type=online"
				+ "&prompt=select_account";
	}
}
