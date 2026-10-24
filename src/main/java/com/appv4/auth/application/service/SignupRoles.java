package com.appv4.auth.application.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;

/**
 * Rôles acceptés sur le formulaire d'inscription (SCRUM-121).
 *
 * <p><b>Business :</b> au moins Parent ou Professeur. Le rôle admin ne se
 * coche pas ici. Professeur ne devient pas un rôle, seulement une demande.
 */
public final class SignupRoles {

	public static final String PARENT = "parent";
	public static final String TEACHER = "teacher";

	private SignupRoles() {
	}

	public static Set<String> normalizeOrThrow(List<String> roles) {
		if (roles == null || roles.isEmpty()) {
			throw missing();
		}
		Set<String> normalized = new LinkedHashSet<>();
		for (String role : roles) {
			if (role == null || role.isBlank()) {
				throw missing();
			}
			String value = role.trim().toLowerCase(Locale.ROOT);
			if (!PARENT.equals(value) && !TEACHER.equals(value)) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST,
						"Rôle non autorisé à l'inscription.");
			}
			normalized.add(value);
		}
		return normalized;
	}

	private static AuthDomainException missing() {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST,
				"Choisissez au moins Parent ou Professeur.");
	}
}
