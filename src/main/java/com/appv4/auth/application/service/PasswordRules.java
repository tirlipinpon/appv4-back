package com.appv4.auth.application.service;

import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

import com.appv4.auth.application.exception.AuthDomainException;

/**
 * Règles mot de passe produit (SCRUM-105) : 8 car., maj, min, chiffre, spécial.
 */
public final class PasswordRules {

	private static final Pattern HAS_UPPER = Pattern.compile("[A-Z]");
	private static final Pattern HAS_LOWER = Pattern.compile("[a-z]");
	private static final Pattern HAS_DIGIT = Pattern.compile("[0-9]");
	private static final Pattern HAS_SPECIAL = Pattern.compile("[^A-Za-z0-9]");

	private PasswordRules() {
	}

	public static void validateOrThrow(String password) {
		if (password == null || password.length() < 8) {
			throw new AuthDomainException(
					HttpStatus.BAD_REQUEST.value(),
					"Mot de passe trop court (min. 8 caractères)");
		}
		if (!HAS_UPPER.matcher(password).find()
				|| !HAS_LOWER.matcher(password).find()
				|| !HAS_DIGIT.matcher(password).find()
				|| !HAS_SPECIAL.matcher(password).find()) {
			throw new AuthDomainException(
					HttpStatus.BAD_REQUEST.value(),
					"Mot de passe : majuscule, minuscule, chiffre et caractère spécial requis");
		}
	}
}
