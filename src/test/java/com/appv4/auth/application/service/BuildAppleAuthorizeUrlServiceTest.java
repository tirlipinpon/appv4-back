package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.infrastructure.config.AppleOAuthProperties;

class BuildAppleAuthorizeUrlServiceTest {

	private AppleOAuthProperties properties;
	private BuildAppleAuthorizeUrlService service;

	@BeforeEach
	void setUp() {
		properties = new AppleOAuthProperties();
		properties.setClientId("com.appv4.service");
		properties.setTeamId("TEAM");
		properties.setKeyId("KEY");
		properties.setPrivateKey("pem");
		properties.setCallbackUri("https://api.example.com/api/int/auth/oauth/apple/callback");
		service = new BuildAppleAuthorizeUrlService(properties);
	}

	@Test
	void buildsFormPostUrlWithBackendCallbackAndState() {
		String url = service.build("http://localhost:4201/auth/callback/apple", "signed.state");

		assertThat(url).contains("response_mode=form_post");
		assertThat(url).contains("response_type=code");
		assertThat(url).contains("scope=name+email");
		assertThat(url).contains("state=signed.state");
		assertThat(url).contains("redirect_uri="
				+ java.net.URLEncoder.encode(
						properties.getCallbackUri(), java.nio.charset.StandardCharsets.UTF_8));
		assertThat(url).doesNotContain("response_mode=query");
	}

	@Test
	void rejectsWhenCallbackMissing() {
		properties.setCallbackUri("");
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
