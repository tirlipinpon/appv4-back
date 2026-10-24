package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;

/**
 * Accès unique au principal JWT des contrôleurs authentifiés.
 *
 * <p><b>Business :</b> sans badge valide, pas d'action métier — même message partout.
 * <p><b>Technique :</b> à utiliser avec {@link AuthenticationPrincipal} ; évite de
 * recopier {@code SecurityContextHolder} dans chaque controller (issue #48).
 * <p><b>Métier / flow :</b> filtre JWT pose le principal → controller injecte →
 * {@link #require} refuse si absent → use case.
 */
public final class CurrentAuth {

	static final String REQUIRED = "Connexion requise.";

	private CurrentAuth() {
	}

	/**
	 * @param user principal injecté, ou {@code null} si anonyme / type inattendu
	 * @return le même user, jamais null
	 */
	public static AuthenticatedUser require(AuthenticatedUser user) {
		if (user == null) {
			throw new AuthDomainException(AuthErrorCode.UNAUTHORIZED, REQUIRED);
		}
		return user;
	}
}
