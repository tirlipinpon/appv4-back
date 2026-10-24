package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.infrastructure.config.AppleOAuthProperties;

class ResumeAppleOAuthCallbackServiceTest {

	private ResumeAppleOAuthCallbackService service;

	@BeforeEach
	void setUp() {
		service = new ResumeAppleOAuthCallbackService(new AppleOAuthProperties());
	}

	@Test
	void redirectsToSpaWithCode() {
		String state = AppleOAuthReturnUris.encodeState("http://localhost:4201/auth/callback/apple");
		URI location = service.resume(state, "auth-code", null);
		assertThat(location.toString())
				.isEqualTo("http://localhost:4201/auth/callback/apple?code=auth-code");
	}

	@Test
	void redirectsWithError() {
		String state = AppleOAuthReturnUris.encodeState("http://localhost:4201/auth/callback/apple");
		URI location = service.resume(state, null, "user_cancelled");
		assertThat(location.toString()).contains("error=user_cancelled");
	}

	@Test
	void rejectsBadState() {
		assertThatThrownBy(() -> service.resume("!!!", "code", null))
				.isInstanceOf(AuthDomainException.class);
	}
}
