package com.appv4.auth.application.service;

import org.springframework.http.HttpStatus;

import com.appv4.auth.application.exception.AuthDomainException;

/**
 * Limites du nom affiché alignées sur {@code back_users.display_name} (255) — issue #50.
 *
 * <p><b>Business :</b> signup refuse un nom trop long (4xx) ; OAuth adapte sans 500.
 */
public final class DisplayNames {

	public static final int MAX_LENGTH = 255;

	private DisplayNames() {
	}

	/** Signup / saisie utilisateur : trop long → 400. */
	public static String requireForSignup(String displayName, String email) {
		String resolved = resolve(displayName, email);
		if (resolved.length() > MAX_LENGTH) {
			throw new AuthDomainException(
					HttpStatus.BAD_REQUEST.value(),
					"Le nom affiché est trop long (max. " + MAX_LENGTH + " caractères)");
		}
		return resolved;
	}

	/** Profils sociaux : coupe à la limite colonne (évite 500). */
	public static String forProvider(String displayName, String email) {
		String resolved = resolve(displayName, email);
		if (resolved.length() <= MAX_LENGTH) {
			return resolved;
		}
		return resolved.substring(0, MAX_LENGTH);
	}

	private static String resolve(String displayName, String email) {
		if (displayName != null && !displayName.isBlank()) {
			return displayName.trim();
		}
		if (email == null || email.isBlank()) {
			return "user";
		}
		int at = email.indexOf('@');
		return at > 0 ? email.substring(0, at) : email;
	}
}
