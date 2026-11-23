package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerInterceptor;

import com.appv4.auth.application.port.in.SejourUseCase;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Refuse chaque route enfant pendant la Nuit.
 *
 * <p><b>Business :</b> l'enfant reçoit la phrase de fermeture, pas un écran de jeu.
 * Le préflight du navigateur reste libre, sinon cette erreur ne pourrait pas s'afficher.
 * <p><b>Technique :</b> {@link HandlerInterceptor} avant le controller. La phrase
 * vient du cas d'usage, pas d'un second texte.
 * <p><b>Métier / flow :</b> {@code /api/ext/**} → {@link SejourUseCase#refuseDuringNuit}
 * → le conseil d'erreur renvoie le JSON. Les routes parent et professeur ne passent pas ici.
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
		sejour.refuseDuringNuit(bearer(request.getHeader(HttpHeaders.AUTHORIZATION)));
		return true;
	}

	private static String bearer(String authorization) {
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return "";
		}
		return authorization.substring(7).trim();
	}
}
