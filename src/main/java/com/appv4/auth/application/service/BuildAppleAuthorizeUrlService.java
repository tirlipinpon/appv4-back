package com.appv4.auth.application.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;

import com.appv4.auth.application.config.AppleOAuthSettings;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.BuildAppleAuthorizeUrlUseCase;

/**
 * Construit l'URL Sign in with Apple (code + form_post, issue #33).
 */
public class BuildAppleAuthorizeUrlService implements BuildAppleAuthorizeUrlUseCase {

	private final Supplier<AppleOAuthSettings> settings;

	public BuildAppleAuthorizeUrlService(Supplier<AppleOAuthSettings> settings) {
		this.settings = settings;
	}

	public BuildAppleAuthorizeUrlService(AppleOAuthSettings settings) {
		this(() -> settings);
	}

	@Override
	public String build(String frontendResumeUri, String state) {
		AppleOAuthSettings s = settings.get();
		if (!s.isConfigured()) {
			throw new AuthDomainException(
					HttpStatus.SERVICE_UNAVAILABLE.value(),
					"Apple OAuth non configuré (APPLE_CLIENT_ID / TEAM_ID / KEY_ID / PRIVATE_KEY)");
		}
		if (!s.isCallbackConfigured()) {
			throw new AuthDomainException(
					HttpStatus.SERVICE_UNAVAILABLE.value(),
					"Apple OAuth callback non configuré (APPLE_CALLBACK_URI)");
		}
		if (frontendResumeUri == null || frontendResumeUri.isBlank()) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), "redirectUri obligatoire");
		}
		if (state == null || state.isBlank()) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), "state OAuth obligatoire");
		}
		String resume = frontendResumeUri.trim();
		if (!AppleOAuthReturnUris.isAllowed(resume, s.allowedReturnUriPrefixes())) {
			throw new AuthDomainException(
					HttpStatus.BAD_REQUEST.value(),
					"redirectUri non autorisé pour la reprise Apple");
		}
		String scope = URLEncoder.encode("name email", StandardCharsets.UTF_8);
		String encodedRedirect = URLEncoder.encode(s.callbackUri().trim(), StandardCharsets.UTF_8);
		String clientId = URLEncoder.encode(s.clientId(), StandardCharsets.UTF_8);
		String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);
		return s.authorizationUri()
				+ "?client_id=" + clientId
				+ "&redirect_uri=" + encodedRedirect
				+ "&response_type=code"
				+ "&response_mode=form_post"
				+ "&scope=" + scope
				+ "&state=" + encodedState;
	}
}
