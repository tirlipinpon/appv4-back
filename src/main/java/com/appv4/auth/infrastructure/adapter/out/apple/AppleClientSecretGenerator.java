package com.appv4.auth.infrastructure.adapter.out.apple;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.infrastructure.config.AppleOAuthProperties;

import io.jsonwebtoken.Jwts;

/**
 * Génère le {@code client_secret} Apple : JWT ES256 (durée ~20 min).
 *
 * <p><b>Technique :</b> Apple n'accepte pas un secret statique — il faut signer
 * avec la clé .p8 (Team ID + Key ID + Services ID).
 */
public final class AppleClientSecretGenerator {

	private static final Logger LOG = LoggerFactory.getLogger(AppleClientSecretGenerator.class);

	private AppleClientSecretGenerator() {
	}

	public static String generate(AppleOAuthProperties properties) {
		try {
			PrivateKey privateKey = parsePrivateKey(properties.getPrivateKey());
			Instant now = Instant.now();
			return Jwts.builder()
					.header()
					.keyId(properties.getKeyId())
					.and()
					.issuer(properties.getTeamId())
					.issuedAt(Date.from(now))
					.expiration(Date.from(now.plusSeconds(1200)))
					.audience()
					.add("https://appleid.apple.com")
					.and()
					.subject(properties.getClientId())
					.signWith(privateKey, Jwts.SIG.ES256)
					.compact();
		} catch (AuthDomainException ex) {
			throw ex;
		} catch (Exception ex) {
			LOG.warn("Apple client_secret generation failed: {}", ex.toString());
			throw new AuthDomainException(
					HttpStatus.SERVICE_UNAVAILABLE.value(),
					"Impossible de générer client_secret Apple");
		}
	}

	private static PrivateKey parsePrivateKey(String pem) throws Exception {
		String normalized = pem
				.replace("\\n", "\n")
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replaceAll("\\s", "");
		byte[] decoded = Base64.getDecoder().decode(normalized);
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
		return KeyFactory.getInstance("EC").generatePrivate(spec);
	}
}
