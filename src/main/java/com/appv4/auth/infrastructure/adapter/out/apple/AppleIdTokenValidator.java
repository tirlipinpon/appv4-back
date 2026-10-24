package com.appv4.auth.infrastructure.adapter.out.apple;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import com.appv4.auth.application.exception.AuthDomainException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;

/**
 * Validation OIDC de l'{@code id_token} Apple (issue #52).
 *
 * <p><b>Business :</b> ne faire confiance qu'à un jeton signé par Apple, pour notre
 * Services ID, non expiré.
 * <p><b>Technique :</b> JWKS {@code https://appleid.apple.com/auth/keys} + JJWT
 * (signature, iss, aud, exp).
 */
public class AppleIdTokenValidator {

	static final String ISSUER = "https://appleid.apple.com";
	static final String DEFAULT_JWKS_URI = "https://appleid.apple.com/auth/keys";
	private static final long CACHE_TTL_MS = 60 * 60 * 1000L;
	private static final Logger LOG = LoggerFactory.getLogger(AppleIdTokenValidator.class);

	private final RestClient restClient;
	private final String jwksUri;
	private final Map<String, PublicKey> keysByKid = new ConcurrentHashMap<>();
	private volatile long cacheLoadedAtEpochMs;

	public AppleIdTokenValidator(RestClient.Builder restClientBuilder) {
		this(restClientBuilder, DEFAULT_JWKS_URI);
	}

	AppleIdTokenValidator(RestClient.Builder restClientBuilder, String jwksUri) {
		this.restClient = restClientBuilder.build();
		this.jwksUri = jwksUri;
	}

	/**
	 * @return claims validés (sub, email éventuel)
	 */
	public Claims validate(String idToken, String expectedAudience) {
		try {
			return Jwts.parser()
					.keyLocator(keyLocator())
					.requireIssuer(ISSUER)
					.requireAudience(expectedAudience)
					.clockSkewSeconds(60)
					.build()
					.parseSignedClaims(idToken)
					.getPayload();
		} catch (AuthDomainException ex) {
			throw ex;
		} catch (Exception ex) {
			LOG.warn("Apple id_token validation failed: {}", ex.toString());
			throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "id_token Apple invalide");
		}
	}

	private Locator<Key> keyLocator() {
		return header -> {
			if (!(header instanceof JwsHeader jwsHeader)) {
				throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "id_token Apple invalide");
			}
			String kid = jwsHeader.getKeyId();
			if (kid == null || kid.isBlank()) {
				throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "id_token Apple invalide");
			}
			PublicKey key = resolveKey(kid);
			if (key == null) {
				throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "id_token Apple invalide");
			}
			return key;
		};
	}

	private PublicKey resolveKey(String kid) {
		ensureKeysLoaded(false);
		PublicKey key = keysByKid.get(kid);
		if (key != null) {
			return key;
		}
		ensureKeysLoaded(true);
		return keysByKid.get(kid);
	}

	private synchronized void ensureKeysLoaded(boolean force) {
		long now = System.currentTimeMillis();
		if (!force && !keysByKid.isEmpty() && now - cacheLoadedAtEpochMs < CACHE_TTL_MS) {
			return;
		}
		JwksResponse jwks = restClient.get()
				.uri(jwksUri)
				.retrieve()
				.body(JwksResponse.class);
		if (jwks == null || jwks.keys() == null || jwks.keys().isEmpty()) {
			throw new AuthDomainException(
					HttpStatus.SERVICE_UNAVAILABLE.value(),
					"JWKS Apple indisponible");
		}
		Map<String, PublicKey> next = new HashMap<>();
		for (Jwk key : jwks.keys()) {
			if (!"RSA".equalsIgnoreCase(key.kty()) || key.kid() == null) {
				continue;
			}
			try {
				next.put(key.kid(), toRsaPublicKey(key.n(), key.e()));
			} catch (Exception ex) {
				LOG.warn("Skip Apple JWK kid={}: {}", key.kid(), ex.toString());
			}
		}
		keysByKid.clear();
		keysByKid.putAll(next);
		cacheLoadedAtEpochMs = now;
	}

	static PublicKey toRsaPublicKey(String n, String e) throws Exception {
		byte[] modulus = Base64.getUrlDecoder().decode(n);
		byte[] exponent = Base64.getUrlDecoder().decode(e);
		RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1, exponent));
		return KeyFactory.getInstance("RSA").generatePublic(spec);
	}

	/** Tests uniquement — évite un appel réseau JWKS. */
	void replaceKeysForTests(Map<String, PublicKey> keys) {
		keysByKid.clear();
		keysByKid.putAll(keys);
		cacheLoadedAtEpochMs = System.currentTimeMillis();
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record JwksResponse(List<Jwk> keys) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record Jwk(
			String kty,
			String kid,
			String n,
			String e,
			@JsonProperty("use") String use,
			String alg) {
	}
}
