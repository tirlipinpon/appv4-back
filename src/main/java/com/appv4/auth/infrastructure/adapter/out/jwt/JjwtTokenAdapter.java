package com.appv4.auth.infrastructure.adapter.out.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;

import com.appv4.auth.application.port.out.JwtTokenDecoder;
import com.appv4.auth.application.port.out.JwtTokenEncoder;
import com.appv4.auth.application.result.JwtTokenResult;
import com.appv4.auth.infrastructure.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Adaptateur technique JWT (librairie JJWT).
 *
 * <p><b>Métier :</b> après un login réussi, le back donne au front un « badge »
 * (JWT) que le front renvoie à chaque appel admin. Ce badge contient qui est
 * connecté ({@code userId}, {@code email}) et jusqu'à quand il est valide.
 *
 * <p><b>Technique :</b> JWT = 3 parties Base64 (header.payload.signature).
 * La signature HMAC utilise {@code appv4.jwt.secret} : si quelqu'un modifie le
 * payload, la signature ne match plus → token rejeté.
 *
 * <p>Hexagonal : cette classe est un <i>adapter out</i> ; le domaine ne connaît
 * pas JJWT, seulement les ports {@link JwtTokenEncoder} / {@link JwtTokenDecoder}.
 */
public class JjwtTokenAdapter implements JwtTokenEncoder, JwtTokenDecoder {

	private final JwtProperties properties;
	private final SecretKey secretKey;

	public JjwtTokenAdapter(JwtProperties properties) {
		this.properties = properties;
		// HMAC-SHA exige une clé assez longue (sinon IllegalArgumentException au démarrage)
		byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
		this.secretKey = Keys.hmacShaKeyFor(keyBytes);
	}

	/**
	 * Crée un JWT signé.
	 *
	 * @param userId     id {@code back_users.id}
	 * @param email      email du compte
	 * @param activeRole espace choisi, ou {@code null} pour omettre le claim
	 * @param roles      espaces accordés, claim {@code roles} (peut être vide)
	 */
	@Override
	public JwtTokenResult encode(long userId, String email, String activeRole, List<String> roles) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(properties.getExpirationMinutes() * 60);
		JwtBuilder builder = Jwts.builder()
				.issuer(properties.getIssuer())
				// subject = identifiant principal du token (convention JWT)
				.subject(Long.toString(userId))
				.claim("email", email)
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt));
		if (activeRole != null && !activeRole.isBlank()) {
			builder.claim("activeRole", activeRole);
		}
		builder.claim("roles", roles == null ? List.of() : roles);
		String token = builder.signWith(secretKey).compact();
		return new JwtTokenResult(token, expiresAt.getEpochSecond());
	}

	/**
	 * Vérifie signature + issuer + expiration, puis lit userId/email.
	 *
	 * @return empty si token pourri, expiré, mauvais issuer, ou email manquant
	 *         (on ne jette pas d'exception HTTP ici : le filtre traite « pas auth »)
	 */
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
			String activeRole = claims.get("activeRole", String.class);
			if (activeRole != null && activeRole.isBlank()) {
				activeRole = null;
			}
			return Optional.of(new AuthenticatedUser(userId, email, activeRole, readRoles(claims)));
		} catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	private static List<String> readRoles(Claims claims) {
		Object raw = claims.get("roles");
		if (!(raw instanceof List<?> list)) {
			return List.of();
		}
		List<String> roles = new ArrayList<>();
		for (Object item : list) {
			if (item instanceof String value && !value.isBlank()) {
				roles.add(value);
			}
		}
		return List.copyOf(roles);
	}
}
