package com.appv4.auth.infrastructure.adapter.out.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import com.appv4.auth.application.exception.AuthDomainException;

class OAuthRestClientErrorsTest {

	@Test
	void resourceAccessMapsTo503() {
		AuthDomainException ex = OAuthRestClientErrors.map(
				new ResourceAccessException("timeout", new SocketTimeoutException()),
				"Google");
		assertThat(ex.getHttpStatus()).isEqualTo(503);
		assertThat(ex.getMessage()).contains("indisponible");
	}

	@Test
	void otherRestClientMapsTo401WithoutLeakingDetail() {
		AuthDomainException ex = OAuthRestClientErrors.map(
				new RestClientException("https://oauth?client_secret=SECRET_MARKER&code=x"),
				"Google");
		assertThat(ex.getHttpStatus()).isEqualTo(401);
		assertThat(ex.getMessage()).isEqualTo("Échec de connexion Google");
		assertThat(ex.getMessage()).doesNotContain("SECRET_MARKER");
	}
}
