package com.appv4.auth.infrastructure.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuse de démarrer avec un secret JWT de démonstration hors local.
 *
 * <p><b>Business :</b> une clé connue du dépôt ne doit jamais signer des sessions
 * en déploiement.
 * <p><b>Technique :</b> après le bind de {@link JwtProperties}, vérifie secret et TTL.
 * <p><b>Métier / flow :</b> boot Spring → avant le premier login.
 */
@Component
public class JwtPropertiesGuard implements InitializingBean {

	/** Même valeur que le défaut de {@code application-local.yml} — interdite hors local. */
	public static final String LOCAL_DEMO_SECRET = "appv4-local-dev-jwt-secret-change-me-32b";

	/** JJWT HMAC : au moins 256 bits. */
	public static final int MIN_SECRET_BYTES = 32;

	public static final long MIN_EXPIRATION_MINUTES = 1;
	/** Plafond : 7 jours. */
	public static final long MAX_EXPIRATION_MINUTES = 10_080;

	private final JwtProperties jwtProperties;
	private final Environment environment;

	public JwtPropertiesGuard(JwtProperties jwtProperties, Environment environment) {
		this.jwtProperties = jwtProperties;
		this.environment = environment;
	}

	@Override
	public void afterPropertiesSet() {
		validateExpiration(jwtProperties.getExpirationMinutes());
		validateSecret(jwtProperties.getSecret(), isLocalProfile());
	}

	boolean isLocalProfile() {
		return Arrays.asList(environment.getActiveProfiles()).contains("local");
	}

	static void validateExpiration(long expirationMinutes) {
		if (expirationMinutes < MIN_EXPIRATION_MINUTES || expirationMinutes > MAX_EXPIRATION_MINUTES) {
			throw new IllegalStateException(
					"appv4.jwt.expiration-minutes doit être entre "
							+ MIN_EXPIRATION_MINUTES + " et " + MAX_EXPIRATION_MINUTES
							+ " (reçu : " + expirationMinutes + ")");
		}
	}

	static void validateSecret(String secret, boolean localProfile) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException(
					"appv4.jwt.secret manquant : définir APPV4_JWT_SECRET"
							+ (localProfile ? " ou utiliser le profil local" : ""));
		}
		byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"appv4.jwt.secret trop court (min. " + MIN_SECRET_BYTES + " octets UTF-8)");
		}
		if (!localProfile && LOCAL_DEMO_SECRET.equals(secret)) {
			throw new IllegalStateException(
					"Secret JWT de démonstration interdit hors profil local : définir APPV4_JWT_SECRET");
		}
	}
}
