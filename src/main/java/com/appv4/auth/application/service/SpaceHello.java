package com.appv4.auth.application.service;


import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;

/**
 * Textes fixes des portes parent et professeur (SCRUM-127).
 *
 * <p><b>Business :</b> la porte parent ne s'ouvre que si le jeton dit parent,
 * et l'inverse pour professeur. Pas d'espace choisi : les deux portes restent fermées.
 * <p><b>Technique :</b> aucun accès base. Le rôle vient du claim {@code activeRole}.
 * <p><b>Métier / flow :</b> appelé par les GET {@code /api/int/parent/hello}
 * et {@code /api/int/teacher/hello}, après le filtre JWT.
 */
public final class SpaceHello {

	public static final String PARENT_TEXT = "hello parent";
	public static final String TEACHER_TEXT = "hello teacher";
	public static final String DENIED = "Espace non autorisé.";

	private SpaceHello() {
	}

	public static String parentHello(String activeRole) {
		return require(activeRole, ActiveRoles.PARENT, PARENT_TEXT);
	}

	public static String teacherHello(String activeRole) {
		return require(activeRole, ActiveRoles.TEACHER, TEACHER_TEXT);
	}

	private static String require(String activeRole, String expected, String text) {
		if (!expected.equals(activeRole)) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, DENIED);
		}
		return text;
	}
}
