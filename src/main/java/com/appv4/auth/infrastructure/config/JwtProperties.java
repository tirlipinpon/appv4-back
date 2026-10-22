package com.appv4.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration JWT (secret, issuer, durée).
 */
@ConfigurationProperties(prefix = "appv4.jwt")
public class JwtProperties {

	private String secret = "appv4-local-dev-jwt-secret-change-me-32b";
	private String issuer = "appv4-back";
	private long expirationMinutes = 480;

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public long getExpirationMinutes() {
		return expirationMinutes;
	}

	public void setExpirationMinutes(long expirationMinutes) {
		this.expirationMinutes = expirationMinutes;
	}
}
