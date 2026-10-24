package com.appv4.auth.application.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;

import com.appv4.auth.application.config.FacebookOAuthSettings;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.BuildFacebookAuthorizeUrlUseCase;

/**
 * Construit l'URL d'autorisation Facebook (dialog OAuth).
 */
public class BuildFacebookAuthorizeUrlService implements BuildFacebookAuthorizeUrlUseCase {

	private final Supplier<FacebookOAuthSettings> settings;

	public BuildFacebookAuthorizeUrlService(Supplier<FacebookOAuthSettings> settings) {
		this.settings = settings;
	}

	public BuildFacebookAuthorizeUrlService(FacebookOAuthSettings settings) {
		this(() -> settings);
	}

	@Override
	public String build(String redirectUri, String state) {
		FacebookOAuthSettings s = settings.get();
		if (!s.isConfigured()) {
			throw new AuthDomainException(
					HttpStatus.SERVICE_UNAVAILABLE.value(),
					"Facebook OAuth non configuré (FACEBOOK_APP_ID / FACEBOOK_APP_SECRET)");
		}
		if (redirectUri == null || redirectUri.isBlank()) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), "redirectUri obligatoire");
		}
		if (state == null || state.isBlank()) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), "state OAuth obligatoire");
		}
		String scope = URLEncoder.encode("email,public_profile", StandardCharsets.UTF_8);
		String encodedRedirect = URLEncoder.encode(redirectUri.trim(), StandardCharsets.UTF_8);
		String appId = URLEncoder.encode(s.appId(), StandardCharsets.UTF_8);
		String encodedState = URLEncoder.encode(state, StandardCharsets.UTF_8);
		return s.authorizationUri()
				+ "?client_id=" + appId
				+ "&redirect_uri=" + encodedRedirect
				+ "&response_type=code"
				+ "&scope=" + scope
				+ "&state=" + encodedState;
	}
}
