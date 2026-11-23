package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.config.AppleOAuthSettings;
import com.appv4.auth.application.exception.AuthDomainException;

class BuildAppleAuthorizeUrlServiceTest {

	private BuildAppleAuthorizeUrlService service;
	private AppleOAuthSettings settings;

	@BeforeEach
	void setUp() {
		settings = new AppleOAuthSettings(
				"com.appv4.service",
				"https://appleid.apple.com/auth/authorize",
				"https://api.example.com/api/int/auth/oauth/apple/callback",
				List.of("http://localhost:4201/", "http://localhost:4200/"),
				true);
		service = new BuildAppleAuthorizeUrlService(settings);
	}

	@Test
	void buildsFormPostUrlWithBackendCallbackAndState() {
		String url = service.build("http://localhost:4201/auth/callback/apple", "signed.state");

		assertThat(url).contains("response_mode=form_post");
		assertThat(url).contains("state=signed.state");
		assertThat(url).contains("redirect_uri="
				+ java.net.URLEncoder.encode(
						settings.callbackUri(), java.nio.charset.StandardCharsets.UTF_8));
	}

	@Test
	void rejectsWhenCallbackMissing() {
		service = new BuildAppleAuthorizeUrlService(new AppleOAuthSettings(
				"com.appv4.service",
				"https://appleid.apple.com/auth/authorize",
				"",
				List.of("http://localhost:4201/"),
				true));
		assertThatThrownBy(() -> service.build("http://localhost:4201/auth/callback/apple", "s"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("APPLE_CALLBACK_URI");
	}

	@Test
	void rejectsDisallowedResumeUri() {
		assertThatThrownBy(() -> service.build("https://evil.example/phish", "s"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("non autorisé");
	}
}
