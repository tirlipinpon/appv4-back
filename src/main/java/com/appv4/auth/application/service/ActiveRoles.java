package com.appv4.auth.application.service;

/**
 * Espaces que l'admin peut ouvrir (SCRUM-124).
 *
 * <p><b>Business :</b> seul parent ou professeur est un espace. Admin n'est pas
 * un choix de navigation ici.
 */
public final class ActiveRoles {

	public static final String PARENT = "parent";
	public static final String TEACHER = "teacher";

	private ActiveRoles() {
	}

	public static boolean isSpace(String role) {
		return PARENT.equals(role) || TEACHER.equals(role);
	}
}
