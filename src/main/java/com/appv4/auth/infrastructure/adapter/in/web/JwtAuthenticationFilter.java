package com.appv4.auth.infrastructure.adapter.in.web;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.appv4.auth.application.port.out.JwtTokenDecoder;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lit {@code Authorization: Bearer} et peuple le SecurityContext.
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
			String raw = header.substring(7).trim();
			jwtTokenDecoder.decode(raw).ifPresent(user -> {
				var auth = new UsernamePasswordAuthenticationToken(
						user,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER")));
				SecurityContextHolder.getContext().setAuthentication(auth);
			});
		}
		filterChain.doFilter(request, response);
	}
}
