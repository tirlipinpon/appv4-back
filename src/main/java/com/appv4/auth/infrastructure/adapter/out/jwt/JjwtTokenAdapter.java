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
	 * @param userId id {@code back_users.id}
	 * @param email  email normalisé (minuscules côté métier plus tard)
	 */
	@Override
	public JwtTokenResult encode(long userId, String email) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(properties.getExpirationMinutes() * 60);
		String token = Jwts.builder()
				.issuer(properties.getIssuer())
				// subject = identifiant principal du token (convention JWT)
				.subject(Long.toString(userId))
				.claim("email", email)
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.signWith(secretKey)
				.compact();
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
			return Optional.of(new AuthenticatedUser(userId, email));
		} catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}
}
