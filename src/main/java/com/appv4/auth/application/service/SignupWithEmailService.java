package com.appv4.auth.application.service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.in.SignupWithEmailUseCase;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserSignupState;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Inscription email/mdp + collision comptes sociaux (SCRUM-105).
 *
 * <p><b>Business :</b> 1 email = 1 compte. Si un compte social existe déjà →
 * refus avec message orientant vers social / mot de passe oublié.
 * <p><b>Hors scope :</b> rôles parent/teacher (epic Roles).
 */
public class SignupWithEmailService implements SignupWithEmailUseCase {

	private final AuthUserAccountPort authUserAccountPort;
	private final PasswordEncoder passwordEncoder;
	private final IssueJwtUseCase issueJwtUseCase;

	public SignupWithEmailService(
			AuthUserAccountPort authUserAccountPort,
			PasswordEncoder passwordEncoder,
			IssueJwtUseCase issueJwtUseCase) {
		this.authUserAccountPort = authUserAccountPort;
		this.passwordEncoder = passwordEncoder;
		this.issueJwtUseCase = issueJwtUseCase;
	}

	@Override
	@Transactional
	public JwtTokenResult signup(String email, String password, String displayName) {
		String normalizedEmail = normalizeEmail(email);
		PasswordRules.validateOrThrow(password);
		String name = resolveDisplayName(displayName, normalizedEmail);

		authUserAccountPort.findSignupStateByEmail(normalizedEmail).ifPresent(this::rejectExisting);

		String hash = passwordEncoder.encode(password);
		AuthUserAccount account = authUserAccountPort.createWithPassword(normalizedEmail, name, hash);
		return issueJwtUseCase.issue(account.id(), account.email());
	}

	private void rejectExisting(AuthUserSignupState existing) {
		if (existing.hasPassword()) {
			throw new AuthDomainException(
					HttpStatus.CONFLICT.value(),
					"Impossible de créer le compte");
		}
		List<String> social = existing.socialProviders();
		if (!social.isEmpty()) {
			String labels = social.stream()
					.map(SignupWithEmailService::prettyProvider)
					.collect(Collectors.joining(", "));
			throw new AuthDomainException(
					HttpStatus.CONFLICT.value(),
					"Un compte existe déjà avec " + labels
							+ ". Connectez-vous via ce fournisseur, ou utilisez Mot de passe oublié"
							+ " pour définir un mot de passe sur ce compte.");
		}
		throw new AuthDomainException(
				HttpStatus.CONFLICT.value(),
				"Impossible de créer le compte");
	}

	private static String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), "Email obligatoire");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static String resolveDisplayName(String displayName, String email) {
		if (displayName != null && !displayName.isBlank()) {
			return displayName.trim();
		}
		int at = email.indexOf('@');
		return at > 0 ? email.substring(0, at) : email;
	}

	private static String prettyProvider(String provider) {
		return switch (provider) {
			case "google" -> "Google";
			case "facebook" -> "Facebook";
			case "apple" -> "Apple";
			default -> provider;
		};
	}
}
