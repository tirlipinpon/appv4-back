package com.appv4.auth.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.transaction.annotation.Transactional;
import com.appv4.auth.application.port.in.ForgotPasswordUseCase;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.PasswordResetMailPort;
import com.appv4.auth.application.port.out.PasswordResetTokenPort;
import com.appv4.auth.infrastructure.config.PasswordResetProperties;

/**
 * Forgot-password : jeton hashé + email lien (SCRUM-108).
 *
 * <p><b>Business :</b> message unique que l'email existe ou non (anti-énumération).
 * <p><b>Technique :</b> token clair aléatoire → SHA-256 en base ; lien FE avec token clair.
 * <p><b>Métier / flow :</b> port in → find email → invalidate anciens → insert → mail.
 */
public class ForgotPasswordService implements ForgotPasswordUseCase {

	public static final String PUBLIC_MESSAGE =
			"Si un compte existe pour cet email, un lien de réinitialisation a été envoyé.";

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final AuthUserAccountPort authUserAccountPort;
	private final PasswordResetTokenPort passwordResetTokenPort;
	private final PasswordResetMailPort passwordResetMailPort;
	private final PasswordResetProperties properties;

	public ForgotPasswordService(
			AuthUserAccountPort authUserAccountPort,
			PasswordResetTokenPort passwordResetTokenPort,
			PasswordResetMailPort passwordResetMailPort,
			PasswordResetProperties properties) {
		this.authUserAccountPort = authUserAccountPort;
		this.passwordResetTokenPort = passwordResetTokenPort;
		this.passwordResetMailPort = passwordResetMailPort;
		this.properties = properties;
	}

	@Override
	@Transactional
	public String requestReset(String email) {
		String normalized = Emails.requireNormalized(email);
		authUserAccountPort.findByEmail(normalized).ifPresent(account -> {
			passwordResetTokenPort.invalidateUnusedForUser(account.id());
			String clearToken = generateClearToken();
			String hash = sha256Hex(clearToken);
			Instant expiresAt = Instant.now().plus(properties.getTokenTtl());
			passwordResetTokenPort.create(account.id(), hash, expiresAt);
			String resetUrl = properties.getFrontendResetUrl() + "?token=" + clearToken;
			passwordResetMailPort.sendResetLink(account.email(), resetUrl);
		});
		return PUBLIC_MESSAGE;
	}

	private static String generateClearToken() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	static String sha256Hex(String clearToken) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(clearToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 indisponible", ex);
		}
	}
}
