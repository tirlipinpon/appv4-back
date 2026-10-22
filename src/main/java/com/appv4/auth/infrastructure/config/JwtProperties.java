package com.appv4.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config bindée depuis {@code application.yml} → {@code appv4.jwt.*}.
 *
 * <p>En local : secret de démo dans le yaml. En prod : obligatoire via
 * variable d'environnement {@code APPV4_JWT_SECRET} (secret Azure / GitHub),
 * jamais committer un vrai secret.
 */
@ConfigurationProperties(prefix = "appv4.jwt")
public class JwtProperties {

	private String secret = "appv4-local-dev-jwt-secret-change-me-32b";
	private String issuer = "appv4-back";
	/** Durée de vie du badge JWT (minutes). Défaut 480 = 8 h. */
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
