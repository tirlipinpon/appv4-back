package com.appv4.auth.infrastructure.adapter.out.oauth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import com.appv4.auth.application.exception.AuthDomainException;

/**
 * Traduit les erreurs HTTP OAuth (issues #57 / #53).
 *
 * <p><b>Business :</b> le client reçoit un message stable, jamais l'URI (client_secret /
 * access_token) ni le corps brut du fournisseur.
 * <p><b>Technique :</b> log serveur avec cause ; {@link ResourceAccessException} → 503.
 */
public final class OAuthRestClientErrors {

	private static final Logger LOG = LoggerFactory.getLogger(OAuthRestClientErrors.class);

	private OAuthRestClientErrors() {
	}

	public static AuthDomainException map(RestClientException ex, String providerLabel) {
		LOG.warn("{} OAuth call failed: {}", providerLabel, safeDetail(ex), ex);
		if (ex instanceof ResourceAccessException) {
			return new AuthDomainException(
					HttpStatus.SERVICE_UNAVAILABLE.value(),
					providerLabel + " indisponible ou délai dépassé");
		}
		return new AuthDomainException(
				HttpStatus.UNAUTHORIZED.value(),
				"Échec de connexion " + providerLabel);
	}

	/** Détail pour logs uniquement — tronqué, sans obligation de redaction parfaite. */
	static String safeDetail(Throwable ex) {
		String detail = ex.getMessage();
		if (detail == null || detail.isBlank()) {
			return ex.getClass().getSimpleName();
		}
		if (detail.length() > 200) {
			return detail.substring(0, 200) + "…";
		}
		return detail;
	}
}
