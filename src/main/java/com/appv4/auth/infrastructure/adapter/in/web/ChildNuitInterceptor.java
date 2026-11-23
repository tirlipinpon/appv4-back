package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;

import com.appv4.auth.application.port.in.SejourUseCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Refuse chaque route enfant pendant la Nuit, et après un séjour oublié.
 *
 * <p><b>Business :</b> l'enfant reçoit la phrase de fermeture, pas un écran de jeu.
 * Après vingt-cinq minutes sans « Je continue », c'est la phrase de déconnexion.
 * L'entrée de jour reste ouverte : c'est elle qui permet un nouveau séjour.
 * Le préflight du navigateur reste libre, sinon cette erreur ne pourrait pas s'afficher.
 * <p><b>Technique :</b> {@link HandlerInterceptor} avant le controller. La phrase
 * vient du cas d'usage, pas d'un second texte.
 * <p><b>Métier / flow :</b> {@code /api/ext/**} → {@link SejourUseCase#refuseDuringNuit}
 * puis {@link SejourUseCase#refuseAfterIdle}, sauf l'entrée qui doit pouvoir rouvrir.
 * Le conseil d'erreur renvoie le JSON. Les routes parent et professeur ne passent pas ici.
 */
public class ChildNuitInterceptor implements HandlerInterceptor {

	private final SejourUseCase sejour;

	public ChildNuitInterceptor(SejourUseCase sejour) {
		this.sejour = sejour;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		String token = bearer(request.getHeader(HttpHeaders.AUTHORIZATION));
		sejour.refuseDuringNuit(token);
		if (!childLogin(request)) {
			sejour.refuseAfterIdle(token);
		}
		return true;
	}

	/** L'entrée de jour doit rester possible : c'est elle qui rouvre un séjour après l'oubli. */
	private static boolean childLogin(HttpServletRequest request) {
		String uri = request.getRequestURI();
		return uri != null && uri.endsWith("/api/ext/auth/login");
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}
}
