package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.appv4.auth.application.config.AppleOAuthSettings;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.OAuthBrowserBindingPort;

@ExtendWith(MockitoExtension.class)
class ResumeAppleOAuthCallbackServiceTest {

	@Mock
	private OAuthBrowserBindingPort binding;

	private ResumeAppleOAuthCallbackService service;

	@BeforeEach
	void setUp() {
		service = new ResumeAppleOAuthCallbackService(
				new AppleOAuthSettings(
						"id",
						"https://appleid.apple.com/auth/authorize",
						"https://api.example.com/cb",
						List.of("http://localhost:4201/"),
						true),
				binding);
	}

	@Test
	void redirectsToSpaWithCodeAndState() {
		when(binding.peek(OAuthBrowserBindingPort.PROVIDER_APPLE, "signed"))
				.thenReturn("http://localhost:4201/auth/callback/apple");
		URI location = service.resume("signed", "auth-code", null);
		assertThat(location.toString())
				.isEqualTo("http://localhost:4201/auth/callback/apple?code=auth-code&state=signed");
	}

	@Test
	void redirectsWithError() {
		when(binding.peek(OAuthBrowserBindingPort.PROVIDER_APPLE, "signed"))
				.thenReturn("http://localhost:4201/auth/callback/apple");
		URI location = service.resume("signed", null, "user_cancelled");
		assertThat(location.toString()).contains("error=user_cancelled");
	}

	@Test
	void rejectsWhenPeekFails() {
		when(binding.peek(OAuthBrowserBindingPort.PROVIDER_APPLE, "bad"))
				.thenThrow(new AuthDomainException(401, "state OAuth invalide"));
		assertThatThrownBy(() -> service.resume("bad", "code", null))
				.isInstanceOf(AuthDomainException.class);
	}
}
