package com.appv4.auth.application.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.ResetPasswordUseCase;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.PasswordResetTokenPort;
import com.appv4.auth.application.port.out.PasswordResetTokenPort.ValidResetToken;

/**
 * Renew mot de passe (SCRUM-111).
 *
 * <p><b>Business :</b> un lien one-shot change le mdp ; ancien lien inutilisable ensuite.
 * <p><b>Technique :</b> SHA-256 → SELECT valide → {@code tryMarkUsed} atomique
 * (issue #43) → BCrypt → invalider les autres jetons.
 * <p><b>Métier / flow :</b> {@code updatePasswordHash} incrémente {@code security_version}
 * → les JWT déjà émis sont refusés au prochain appel (issue #49).
 */
public class ResetPasswordService implements ResetPasswordUseCase {

	public static final String INVALID_TOKEN_MESSAGE = "Lien invalide ou expiré";

	public static final String SUCCESS_MESSAGE = "Mot de passe mis à jour.";

	private final PasswordResetTokenPort passwordResetTokenPort;
	private final AuthUserAccountPort authUserAccountPort;
	private final PasswordEncoder passwordEncoder;

	public ResetPasswordService(
			PasswordResetTokenPort passwordResetTokenPort,
			AuthUserAccountPort authUserAccountPort,
			PasswordEncoder passwordEncoder) {
		this.passwordResetTokenPort = passwordResetTokenPort;
		this.authUserAccountPort = authUserAccountPort;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	@Transactional
	public void reset(String clearToken, String newPassword) {
		if (clearToken == null || clearToken.isBlank()) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), INVALID_TOKEN_MESSAGE);
		}
		PasswordRules.validateOrThrow(newPassword);

		String hash = ForgotPasswordService.sha256Hex(clearToken.trim());
		ValidResetToken token = passwordResetTokenPort.findValidByTokenHash(hash)
				.orElseThrow(() -> new AuthDomainException(
						HttpStatus.BAD_REQUEST.value(),
						INVALID_TOKEN_MESSAGE));
		// Consommer avant d'écrire le mdp : un seul concurrent gagne.
		if (!passwordResetTokenPort.tryMarkUsed(token.tokenId())) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), INVALID_TOKEN_MESSAGE);
		}

		String passwordHash = passwordEncoder.encode(newPassword);
		authUserAccountPort.updatePasswordHash(token.userId(), passwordHash);
		// Autres jetons reset encore ouverts pour ce user → inutilisables
		passwordResetTokenPort.invalidateUnusedForUser(token.userId());
	}
}
