package com.appv4.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limite de débit sur les parcours auth publics (login, signup, forgot, OAuth).
 *
 * <p><b>Business :</b> freiner les essais de mots de passe et le spam forgot-password
 * qui invalide les liens précédents (issue #59).
 * <p><b>Technique :</b> fenêtre glissante en mémoire par IP + chemin ; hors multi-instance
 * (gateway/WAF peut compléter en prod).
 */
@ConfigurationProperties(prefix = "appv4.auth.rate-limit")
public class AuthRateLimitProperties {

	/** Si false, le filtre laisse tout passer (tests de charge contrôlés). */
	private boolean enabled = true;

	/** Nombre max de requêtes acceptées dans la fenêtre. */
	private int maxRequests = 30;

	/** Durée de la fenêtre en secondes. */
	private int windowSeconds = 60;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getMaxRequests() {
		return maxRequests;
	}

	public void setMaxRequests(int maxRequests) {
		this.maxRequests = maxRequests;
	}

	public int getWindowSeconds() {
		return windowSeconds;
	}

	public void setWindowSeconds(int windowSeconds) {
		this.windowSeconds = windowSeconds;
	}
}
