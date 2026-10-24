package com.appv4.auth.application.service;

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
 * <p><b>Technique :</b> {@code matches} toujours appelé, y compris sur un hash factice
 * si le compte est absent / sans mdp (issue #38 — coût comparable). Rate-limit HTTP en plus (#59).
 * <p><b>Métier / flow :</b> port in → port out user → IssueJwt si OK.
 */
public class LoginWithEmailService implements LoginWithEmailUseCase {

	public static final String INVALID_CREDENTIALS_MESSAGE =
			"Email ou mot de passe inconnu ou errone.";

	/**
	 * Hash BCrypt valide (coût 10) d'une valeur qui n'est jamais un vrai mdp utilisateur.
	 * Sert uniquement à égaliser le coût quand le compte n'existe pas ou n'a pas de hash.
	 */
	static final String DUMMY_BCRYPT_HASH =
			"$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

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
		String normalized = Emails.requireNormalized(email);
		if (password == null || password.isBlank()) {
			throw invalidCredentials();
		}

		var candidate = authUserAccountPort.findLoginCandidateByEmail(normalized);
		String hash = candidate
				.map(AuthUserLoginCandidate::passwordHash)
				.filter(h -> h != null && !h.isBlank())
				.orElse(DUMMY_BCRYPT_HASH);

		boolean matches = passwordEncoder.matches(password, hash);
		if (candidate.isEmpty() || !matches) {
			throw invalidCredentials();
		}

		AuthUserLoginCandidate user = candidate.get();
		return issueJwtUseCase.issue(user.id(), user.email());
	}

	private AuthDomainException invalidCredentials() {
		return new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), INVALID_CREDENTIALS_MESSAGE);
	}
}
