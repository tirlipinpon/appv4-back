package com.appv4.auth.infrastructure.adapter.out.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.result.JwtTokenResult;
import com.appv4.auth.infrastructure.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Jeton de l'app enfant (BACK-13).
 *
 * <p><b>Business :</b> après un prénom et un code reconnus, l'enfant reçoit un badge
 * qui n'ouvre pas l'admin.
 * <p><b>Technique :</b> HMAC avec le même secret que le JWT adulte, claim {@code kind=child},
 * sujet {@code child:{id}}. {@link JjwtTokenAdapter} refuse ce sujet.
 * <p><b>Métier / flow :</b> adapter out du port {@link ChildSessionTokenPort}.
 */
public class JjwtChildSessionAdapter implements ChildSessionTokenPort {

	static final String KIND = "child";
	private static final String SUBJECT_PREFIX = "child:";

	private final JwtProperties properties;
	private final SecretKey secretKey;

	public JjwtChildSessionAdapter(JwtProperties properties) {
		this.properties = properties;
		byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
	}

	@Override
	public JwtTokenResult issue(long childId, String firstName) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(properties.getExpirationMinutes() * 60);
		String token = Jwts.builder()
				.issuer(properties.getIssuer())
				.subject(SUBJECT_PREFIX + childId)
				.claim("kind", KIND)
				.claim("firstName", firstName)
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.signWith(secretKey)
				.compact();
		return new JwtTokenResult(token, expiresAt.getEpochSecond());
	}

	@Override
	public Optional<Instant> issuedAt(String rawToken) {
		return claims(rawToken).map(payload -> payload.getIssuedAt().toInstant());
	}

	@Override
	public Optional<ChildSession> read(String rawToken) {
		return claims(rawToken).map(payload -> {
			String subject = payload.getSubject();
			long childId = Long.parseLong(subject.substring(SUBJECT_PREFIX.length()));
			return new ChildSession(childId);
		});
	}

	private Optional<Claims> claims(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			return Optional.empty();
		}
		try {
			Claims payload = Jwts.parser()
					.verifyWith(secretKey)
					.requireIssuer(properties.getIssuer())
					.build()
					.parseSignedClaims(rawToken)
					.getPayload();
			if (!KIND.equals(payload.get("kind", String.class))) {
				return Optional.empty();
			}
			String subject = payload.getSubject();
			if (subject == null || !subject.startsWith(SUBJECT_PREFIX)) {
				return Optional.empty();
			}
			if (payload.getIssuedAt() == null) {
				return Optional.empty();
			}
			return Optional.of(payload);
		} catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}
}
