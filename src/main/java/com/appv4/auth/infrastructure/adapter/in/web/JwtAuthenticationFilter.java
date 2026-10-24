package com.appv4.auth.infrastructure.adapter.in.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.SessionState;
import com.appv4.auth.application.port.out.JwtTokenDecoder;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtre HTTP exécuté sur chaque requête.
 *
 * <p><b>Métier :</b> si le front envoie un jeton de session (JWT), on reconnaît
 * l'utilisateur pour cette requête. Sinon on ne bloque pas ici : c'est
 * {@code SecurityFilterChain} qui renverra 401 sur {@code /api/int/**}.
 *
 * <p><b>Business :</b> un badge reste valable seulement si le compte est actif,
 * la version de sécurité du jeton matche la base, et l'espace déclaré est encore tenu
 * (issue #49 — révocation / reset mdp).
 *
 * <p><b>Technique :</b> pattern {@link OncePerRequestFilter} (une fois par requête).
 * Header attendu : {@code Authorization: Bearer eyJhbGciOi...}
 * Puis relecture légère {@link AuthUserAccountPort#findSessionState} + rôles.
 *
 * <p>Si le token est invalide / expiré / révoqué : on n'authentifie pas (Optional vide
 * ou contrôle session KO) → la chaîne Security traite la requête comme anonyme.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenDecoder jwtTokenDecoder;
	private final AuthUserAccountPort authUserAccountPort;
	private final ActiveRolePort activeRolePort;

	public JwtAuthenticationFilter(
			JwtTokenDecoder jwtTokenDecoder,
			AuthUserAccountPort authUserAccountPort,
			ActiveRolePort activeRolePort) {
		this.jwtTokenDecoder = jwtTokenDecoder;
		this.authUserAccountPort = authUserAccountPort;
		this.activeRolePort = activeRolePort;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null && header.startsWith("Bearer ")) {
			// "Bearer " = 7 caractères → le reste est le JWT brut
			String raw = header.substring(7).trim();
			jwtTokenDecoder.decode(raw).ifPresent(this::authenticateIfSessionStillValid);
		}
		// Toujours continuer la chaîne (controllers, autres filtres…)
		filterChain.doFilter(request, response);
	}

	private void authenticateIfSessionStillValid(AuthenticatedUser user) {
		SessionState session = authUserAccountPort.findSessionState(user.userId()).orElse(null);
		if (session == null || !session.active() || session.securityVersion() != user.securityVersion()) {
			return;
		}
		if (user.activeRole() != null
				&& !activeRolePort.findSpaceRoles(user.userId()).contains(user.activeRole())) {
			return;
		}
		List<SimpleGrantedAuthority> authorities = new ArrayList<>();
		authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		// Admin relu en base : un claim JWT seul ne suffit plus après une révocation.
		if (activeRolePort.hasAdminRole(user.userId())) {
			authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
		}
		var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
		SecurityContextHolder.getContext().setAuthentication(auth);
	}
}
