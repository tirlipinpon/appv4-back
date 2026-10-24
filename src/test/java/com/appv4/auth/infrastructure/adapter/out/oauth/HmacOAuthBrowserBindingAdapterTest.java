package com.appv4.auth.infrastructure.adapter.out.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.OAuthBrowserBindingPort;
import com.appv4.auth.infrastructure.config.JwtProperties;

class HmacOAuthBrowserBindingAdapterTest {

	private HmacOAuthBrowserBindingAdapter adapter;

	@BeforeEach
	void setUp() {
		JwtProperties props = new JwtProperties();
		props.setSecret("appv4-test-jwt-secret-not-for-production-32b");
		adapter = new HmacOAuthBrowserBindingAdapter(props, Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void issuePeekConsumeRoundTrip() {
		String state = adapter.issue(OAuthBrowserBindingPort.PROVIDER_GOOGLE, "http://localhost:4201/cb");
		assertThat(adapter.peek(OAuthBrowserBindingPort.PROVIDER_GOOGLE, state))
				.isEqualTo("http://localhost:4201/cb");
		adapter.consume(OAuthBrowserBindingPort.PROVIDER_GOOGLE, "http://localhost:4201/cb", state);
		assertThatThrownBy(() -> adapter.consume(
						OAuthBrowserBindingPort.PROVIDER_GOOGLE, "http://localhost:4201/cb", state))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("déjà utilisé");
	}

	@Test
	void rejectsWrongRedirect() {
		String state = adapter.issue(OAuthBrowserBindingPort.PROVIDER_GOOGLE, "http://localhost:4201/a");
		assertThatThrownBy(() -> adapter.consume(
						OAuthBrowserBindingPort.PROVIDER_GOOGLE, "http://localhost:4201/b", state))
				.isInstanceOf(AuthDomainException.class);
	}
}
