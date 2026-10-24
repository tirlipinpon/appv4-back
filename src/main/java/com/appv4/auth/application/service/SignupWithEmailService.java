package com.appv4.auth.application.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.in.SignupWithEmailUseCase;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Inscription email/mdp + collision comptes (SCRUM-105).
 *
 * <p><b>Business :</b> 1 email = 1 compte. En cas de collision, message générique
 * (pas de liste des fournisseurs liés — anti-énumération, issue #45).
 * Le rôle n'est pas choisi ici (SCRUM-153).
 */
public class SignupWithEmailService implements SignupWithEmailUseCase {

	/** Message public unique pour toute collision d'email (local ou social). */
	public static final String ACCOUNT_CONFLICT_MESSAGE = "Impossible de créer le compte";

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
		String name = DisplayNames.requireForSignup(displayName, normalizedEmail);

		if (authUserAccountPort.findSignupStateByEmail(normalizedEmail).isPresent()) {
			throw new AuthDomainException(HttpStatus.CONFLICT.value(), ACCOUNT_CONFLICT_MESSAGE);
		}

		String hash = passwordEncoder.encode(password);
		AuthUserAccount account = authUserAccountPort.createWithPassword(normalizedEmail, name, hash);
		return issueJwtUseCase.issue(account.id(), account.email());
	}

	private static String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), "Email obligatoire");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
