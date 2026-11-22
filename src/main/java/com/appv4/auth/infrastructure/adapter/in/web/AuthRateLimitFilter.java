package com.appv4.auth.infrastructure.adapter.in.web;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import com.appv4.auth.application.service.RecoverIdentifierService;
import com.appv4.auth.infrastructure.config.AuthRateLimitProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtre anti-abus sur les endpoints auth publics.
 *
 * <p><b>Business :</b> sans ça, login / forgot / OAuth / code enfant peuvent être martelés
 * (bruteforce, invalidation en boucle des liens reset). Le code enfant n'a que 4 chiffres.
 * <p><b>Technique :</b> {@link OncePerRequestFilter} + {@link InMemoryAuthRateLimiter} ;
 * réponse {@code 429 Too Many Requests} si plafond dépassé.
 * <p><b>Métier / flow :</b> avant JWT / controllers, sur les chemins listés uniquement.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {

	private static final Set<String> PROTECTED_PREFIXES = Set.of(
			"/api/int/auth/login",
			"/api/int/auth/signup",
			"/api/int/auth/forgot-password",
			"/api/int/auth/recover-identifier",
			"/api/int/auth/reset-password",
			"/api/int/auth/oauth/google",
			"/api/int/auth/oauth/facebook",
			"/api/int/auth/oauth/apple",
			"/api/ext/auth/login");

	private final AuthRateLimitProperties properties;
	private final InMemoryAuthRateLimiter limiter;

	public AuthRateLimitFilter(AuthRateLimitProperties properties, InMemoryAuthRateLimiter limiter) {
		this.properties = properties;
		this.limiter = limiter;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (!properties.isEnabled()) {
			return true;
		}
		String path = normalizedPath(request);
		for (String prefix : PROTECTED_PREFIXES) {
			if (path.equals(prefix) || path.startsWith(prefix + "/")) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String key = clientKey(request) + "|" + normalizedPath(request);
		if (!limiter.tryAcquire(key, System.currentTimeMillis())) {
			if (normalizedPath(request).equals("/api/int/auth/recover-identifier")) {
				response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				response.getWriter().write(
						"{\"message\":\"" + RecoverIdentifierService.WAIT + "\"}");
				return;
			}
			response.sendError(
					HttpStatus.TOO_MANY_REQUESTS.value(),
					HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
			return;
		}
		filterChain.doFilter(request, response);
	}

	private static String normalizedPath(HttpServletRequest request) {
		String uri = request.getRequestURI();
		String context = request.getContextPath();
		if (context != null && !context.isEmpty() && uri.startsWith(context)) {
			uri = uri.substring(context.length());
		}
		if (uri.length() > 1 && uri.endsWith("/")) {
			return uri.substring(0, uri.length() - 1);
		}
		return uri;
	}

	private static String clientKey(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			int comma = forwarded.indexOf(',');
			return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
		}
		String remote = request.getRemoteAddr();
		return remote != null ? remote : "unknown";
	}
}
