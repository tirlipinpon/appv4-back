package com.appv4.auth.application.service;

/**
 * Espaces que la personne peut ouvrir (SCRUM-124).
 *
 * <p><b>Business :</b> seul parent ou professeur est un espace.
 * {@code admin} modère les demandes, ce n'est pas une pièce de l'app.
 */
public final class ActiveRoles {

	public static final String PARENT = "parent";
	public static final String TEACHER = "teacher";
	public static final String ADMIN = "admin";

	private ActiveRoles() {
	}

	public static boolean isSpace(String role) {
		return PARENT.equals(role) || TEACHER.equals(role);
	}
}
