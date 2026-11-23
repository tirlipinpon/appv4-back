package com.appv4.auth.application.result;

import java.util.List;

/**
 * Ce que l'en-tête doit savoir pour proposer le bon bouton (SCRUM-152).
 *
 * @param roles          espaces déjà accordés ({@code parent}, {@code teacher})
 * @param teacherRequest {@code none}, {@code pending}, {@code approved} ou {@code revoked}
 */
public record SpaceAccess(List<String> roles, String teacherRequest) {

	public SpaceAccess {
		roles = roles == null ? List.of() : List.copyOf(roles);
	}
}
