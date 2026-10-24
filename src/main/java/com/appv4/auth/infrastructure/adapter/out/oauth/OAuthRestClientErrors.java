package com.appv4.auth.infrastructure.adapter.out.oauth;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import com.appv4.auth.application.exception.AuthDomainException;

/**
 * Traduit les erreurs HTTP OAuth (issue #57).
 *
 * <p><b>Business :</b> timeout / réseau → fournisseur indisponible ; autre → code invalide.
 * <p><b>Technique :</b> {@link ResourceAccessException} (connect/read timeout, DNS…) → 503.
 */
public final class OAuthRestClientErrors {

	private OAuthRestClientErrors() {
	}

	public static AuthDomainException map(RestClientException ex, String providerLabel) {
		if (ex instanceof ResourceAccessException) {
			return new AuthDomainException(
					HttpStatus.SERVICE_UNAVAILABLE.value(),
					providerLabel + " indisponible ou délai dépassé");
		}
		String detail = ex.getMessage();
		if (detail == null || detail.isBlank()) {
			detail = ex.getClass().getSimpleName();
		}
		return new AuthDomainException(
				HttpStatus.UNAUTHORIZED.value(),
				"Échec échange code " + providerLabel + " : " + detail);
	}
}
