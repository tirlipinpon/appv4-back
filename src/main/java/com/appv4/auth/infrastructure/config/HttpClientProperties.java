package com.appv4.auth.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Budgets HTTP pour les clients OAuth (issue #57).
 *
 * <p><b>Business :</b> une panne Google/Facebook/Apple ne doit pas bloquer le thread
 * indéfiniment ni être présentée comme un mauvais code.
 * <p><b>Technique :</b> appliqué sur {@code RestClient.Builder} via
 * {@code SimpleClientHttpRequestFactory}.
 */
@ConfigurationProperties(prefix = "appv4.http")
public class HttpClientProperties {

	private Duration connectTimeout = Duration.ofSeconds(5);
	private Duration readTimeout = Duration.ofSeconds(10);

	public Duration getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(Duration connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public Duration getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(Duration readTimeout) {
		this.readTimeout = readTimeout;
	}
}
