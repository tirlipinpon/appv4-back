package com.appv4.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config bindée depuis {@code application.yml} → {@code appv4.jwt.*}.
 *
 * <p><b>Business :</b> sans secret connu seulement de nous, n'importe qui pourrait
 * fabriquer un badge admin.
 * <p><b>Technique :</b> hors profil {@code local}, {@code APPV4_JWT_SECRET} est
 * obligatoire (pas de valeur de démo). Validé au démarrage par {@link JwtPropertiesGuard}.
 * <p><b>Métier / flow :</b> lu par {@code JjwtTokenAdapter} à chaque émission / lecture JWT.
 */
@ConfigurationProperties(prefix = "appv4.jwt")
public class JwtProperties {

	/** Secret HMAC. Vide tant qu'aucune source (env / profil local) ne le pose. */
	private String secret = "";
	private String issuer = "appv4-back";
	/** Durée de vie du badge JWT (minutes). Défaut 480 = 8 h. */
	private long expirationMinutes = 480;

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getIssuer() {
		return issuer;
	}

	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	public long getExpirationMinutes() {
		return expirationMinutes;
	}

	public void setExpirationMinutes(long expirationMinutes) {
		this.expirationMinutes = expirationMinutes;
	}
}
