package com.appv4.auth.infrastructure.adapter.out.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import com.appv4.auth.application.port.out.JwtTokenDecoder;
import com.appv4.auth.application.port.out.JwtTokenEncoder;
import com.appv4.auth.application.result.JwtTokenResult;
import com.appv4.auth.infrastructure.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Adaptateur JJWT — encode et décode les jetons admin.
 */
public class JjwtTokenAdapter implements JwtTokenEncoder, JwtTokenDecoder {

	private final JwtProperties properties;
	private final SecretKey secretKey;

	public JjwtTokenAdapter(JwtProperties properties) {
		this.properties = properties;
		byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
	}

	@Override
	public JwtTokenResult encode(long userId, String email) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(properties.getExpirationMinutes() * 60);
		String token = Jwts.builder()
				.issuer(properties.getIssuer())
				.subject(Long.toString(userId))
				.claim("email", email)
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.signWith(secretKey)
				.compact();
		return new JwtTokenResult(token, expiresAt.getEpochSecond());
	}

	@Override
	public Optional<AuthenticatedUser> decode(String rawToken) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(secretKey)
					.requireIssuer(properties.getIssuer())
					.build()
					.parseSignedClaims(rawToken)
					.getPayload();
			long userId = Long.parseLong(claims.getSubject());
			String email = claims.get("email", String.class);
			if (email == null || email.isBlank()) {
				return Optional.empty();
			}
			return Optional.of(new AuthenticatedUser(userId, email));
		} catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}
}
