package com.appv4.auth.infrastructure.adapter.in.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.appv4.auth.application.port.out.JwtTokenDecoder;
import com.appv4.auth.application.service.ActiveRoles;

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
 * <p><b>Technique :</b> pattern {@link OncePerRequestFilter} (une fois par requête).
 * Header attendu : {@code Authorization: Bearer eyJhbGciOi...}
 *
 * <p>Si le token est invalide / expiré : on n'authentifie pas (Optional vide)
 * → la chaîne Security traitera la requête comme anonyme.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenDecoder jwtTokenDecoder;

	public JwtAuthenticationFilter(JwtTokenDecoder jwtTokenDecoder) {
		this.jwtTokenDecoder = jwtTokenDecoder;
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
			jwtTokenDecoder.decode(raw).ifPresent(user -> {
				// Objet Spring Security = « cet utilisateur est authentifié pour cette requête »
				List<SimpleGrantedAuthority> authorities = new ArrayList<>();
				authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
				if (user.roles().contains(ActiveRoles.ADMIN)) {
					authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
				}
				var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
				SecurityContextHolder.getContext().setAuthentication(auth);
			});
		}
		// Toujours continuer la chaîne (controllers, autres filtres…)
		filterChain.doFilter(request, response);
	}
}
