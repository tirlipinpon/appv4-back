package com.appv4.auth.infrastructure.adapter.out.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;

class OAuthRestClientErrorsTest {

	@Test
	void resourceAccessMapsToServiceUnavailable() {
		AuthDomainException ex = OAuthRestClientErrors.map(
				new ResourceAccessException("timeout", new SocketTimeoutException()),
				"Google");
		assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.SERVICE_UNAVAILABLE);
		assertThat(ex.getMessage()).contains("indisponible");
	}

	@Test
	void otherRestClientMapsToUnauthorizedWithoutLeakingDetail() {
		AuthDomainException ex = OAuthRestClientErrors.map(
				new RestClientException("https://oauth?client_secret=SECRET_MARKER&code=x"),
				"Google");
		assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.UNAUTHORIZED);
		assertThat(ex.getMessage()).isEqualTo("Échec de connexion Google");
		assertThat(ex.getMessage()).doesNotContain("SECRET_MARKER");
	}
}
