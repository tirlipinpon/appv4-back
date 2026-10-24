package com.appv4.auth.application.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;

import com.appv4.auth.application.exception.AuthDomainException;

/**
 * Normalisation email unique (issue #40).
 *
 * <p><b>Business :</b> une même adresse doit matcher quel que soit le parcours
 * (signup, login email, OAuth).
 * <p><b>Technique :</b> {@code trim} + {@link Locale#ROOT} — jamais la locale JVM
 * (ex. turc : {@code I}/{@code ı}).
 */
public final class Emails {

	private Emails() {
	}

	/**
	 * @return email normalisé, jamais null
	 * @throws AuthDomainException 400 si absent
	 */
	public static String requireNormalized(String email) {
		if (email == null || email.isBlank()) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), "Email obligatoire");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * Comme {@link #requireNormalized} mais message métier OAuth si manquant.
	 */
	public static String requireNormalizedOrThrow(String email, String missingMessage) {
		if (email == null || email.isBlank()) {
			throw new AuthDomainException(HttpStatus.FORBIDDEN.value(), missingMessage);
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
