package com.appv4.auth.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config reset mdp (SCRUM-108) — TTL jeton + URL front /reset-password.
 */
@ConfigurationProperties(prefix = "appv4.auth.password-reset")
public class PasswordResetProperties {

	/** Durée de validité du jeton (défaut 45 min). */
	private Duration tokenTtl = Duration.ofMinutes(45);

	/** Base URL Angular admin pour le lien email. */
	private String frontendResetUrl = "http://localhost:4201/reset-password";

	public Duration getTokenTtl() {
		return tokenTtl;
	}

	public void setTokenTtl(Duration tokenTtl) {
		this.tokenTtl = tokenTtl;
	}

	public String getFrontendResetUrl() {
		return frontendResetUrl;
	}

	public void setFrontendResetUrl(String frontendResetUrl) {
		this.frontendResetUrl = frontendResetUrl;
	}
}
