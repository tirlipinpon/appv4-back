package com.appv4.auth.infrastructure.adapter.out.oauth;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatus;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.OAuthBrowserBindingPort;
import com.appv4.auth.infrastructure.config.JwtProperties;

/**
 * State OAuth signé HMAC + anti-rejeu en mémoire (issue #32).
 *
 * <p><b>Business :</b> le navigateur qui a demandé authorize-url doit présenter le même state.
 * <p><b>Technique :</b> {@code payload.b64}.{@code hmac.b64} ; secret = {@code appv4.jwt.secret}.
 * <p><b>Métier / flow :</b> adapter out du port {@link OAuthBrowserBindingPort}.
 */
public class HmacOAuthBrowserBindingAdapter implements OAuthBrowserBindingPort {

	static final Duration TTL = Duration.ofMinutes(10);

	private final byte[] secret;
	private final Clock clock;
	/** jti déjà consommés → expiration pour GC paresseux. */
	private final Map<String, Instant> consumedUntil = new ConcurrentHashMap<>();

	public HmacOAuthBrowserBindingAdapter(JwtProperties jwtProperties) {
		this(jwtProperties, Clock.systemUTC());
	}

	public HmacOAuthBrowserBindingAdapter(JwtProperties jwtProperties, Clock clock) {
		this.secret = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
		this.clock = clock;
	}

	@Override
	public String issue(String provider, String redirectUri) {
		purgeExpired();
		String jti = UUID.randomUUID().toString();
		Instant exp = clock.instant().plus(TTL);
		String payload = provider + "|" + redirectUri.trim() + "|" + exp.getEpochSecond() + "|" + jti;
		String body = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
		return body + "." + sign(body);
	}

	@Override
	public void consume(String provider, String redirectUri, String state) {
		Parsed parsed = parseAndVerify(state);
		assertMatch(parsed, provider, redirectUri);
		Instant prev = consumedUntil.putIfAbsent(parsed.jti(), parsed.expiresAt());
		if (prev != null) {
			throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "state OAuth déjà utilisé");
		}
	}

	@Override
	public String peek(String provider, String state) {
		Parsed parsed = parseAndVerify(state);
		if (!provider.equals(parsed.provider())) {
			throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "state OAuth invalide");
		}
		return parsed.redirectUri();
	}

	private void assertMatch(Parsed parsed, String provider, String redirectUri) {
		if (!provider.equals(parsed.provider())
				|| !redirectUri.trim().equals(parsed.redirectUri())) {
			throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "state OAuth invalide");
		}
	}

	private Parsed parseAndVerify(String state) {
		if (state == null || state.isBlank() || !state.contains(".")) {
			throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "state OAuth manquant");
		}
		int dot = state.indexOf('.');
		String body = state.substring(0, dot);
		String mac = state.substring(dot + 1);
		if (!MessageDigest.isEqual(
				sign(body).getBytes(StandardCharsets.UTF_8),
				mac.getBytes(StandardCharsets.UTF_8))) {
			throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "state OAuth invalide");
		}
		String payload;
		try {
			payload = new String(Base64.getUrlDecoder().decode(body), StandardCharsets.UTF_8);
		} catch (IllegalArgumentException ex) {
			throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "state OAuth invalide");
		}
		String[] parts = payload.split("\\|", 4);
		if (parts.length != 4) {
			throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "state OAuth invalide");
		}
		Instant exp;
		try {
			exp = Instant.ofEpochSecond(Long.parseLong(parts[2]));
		} catch (NumberFormatException ex) {
			throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "state OAuth invalide");
		}
		if (clock.instant().isAfter(exp)) {
			throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "state OAuth expiré");
		}
		return new Parsed(parts[0], parts[1], exp, parts[3]);
	}

	private String sign(String body) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret, "HmacSHA256"));
			byte[] raw = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
		} catch (NoSuchAlgorithmException | InvalidKeyException ex) {
			throw new IllegalStateException("HMAC OAuth state indisponible", ex);
		}
	}

	private void purgeExpired() {
		Instant now = clock.instant();
		consumedUntil.entrySet().removeIf(e -> e.getValue().isBefore(now));
	}

	private record Parsed(String provider, String redirectUri, Instant expiresAt, String jti) {
	}
}
