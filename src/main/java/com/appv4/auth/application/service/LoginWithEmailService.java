package com.appv4.auth.application.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.in.LoginWithEmailUseCase;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserLoginCandidate;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Login email/mdp (SCRUM-117) — anti-énumération.
 *
 * <p><b>Business :</b> email inconnu OU mauvais mdp → même message (ne pas révéler lequel).
 * <p><b>Technique :</b> findLoginCandidate + {@link PasswordEncoder#matches}.
 * <p><b>Métier / flow :</b> port in → port out user → IssueJwt si OK.
 */
public class LoginWithEmailService implements LoginWithEmailUseCase {

	public static final String INVALID_CREDENTIALS_MESSAGE =
			"Email ou mot de passe inconnu ou errone.";

	private final AuthUserAccountPort authUserAccountPort;
	private final PasswordEncoder passwordEncoder;
	private final IssueJwtUseCase issueJwtUseCase;

	public LoginWithEmailService(
			AuthUserAccountPort authUserAccountPort,
			PasswordEncoder passwordEncoder,
			IssueJwtUseCase issueJwtUseCase) {
		this.authUserAccountPort = authUserAccountPort;
		this.passwordEncoder = passwordEncoder;
		this.issueJwtUseCase = issueJwtUseCase;
	}

	@Override
	public JwtTokenResult login(String email, String password) {
		String normalized = normalizeEmail(email);
		if (password == null || password.isBlank()) {
			throw invalidCredentials();
		}

		AuthUserLoginCandidate candidate = authUserAccountPort.findLoginCandidateByEmail(normalized)
				.orElseThrow(this::invalidCredentials);

		String hash = candidate.passwordHash();
		if (hash == null || hash.isBlank() || !passwordEncoder.matches(password, hash)) {
			throw invalidCredentials();
		}

		return issueJwtUseCase.issue(candidate.id(), candidate.email());
	}

	private static String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), "Email obligatoire");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private AuthDomainException invalidCredentials() {
		return new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), INVALID_CREDENTIALS_MESSAGE);
	}
}
