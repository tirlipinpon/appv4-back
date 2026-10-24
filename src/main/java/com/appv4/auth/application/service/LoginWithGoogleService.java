package com.appv4.auth.application.service;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.in.LoginWithGoogleUseCase;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.GoogleOAuthClient;
import com.appv4.auth.application.port.out.GoogleOAuthClient.GoogleUserProfile;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Orchestration login Google : profil → upsert user/identity → JWT.
 */
public class LoginWithGoogleService implements LoginWithGoogleUseCase {

	static final String PROVIDER_GOOGLE = "google";

	private final GoogleOAuthClient googleOAuthClient;
	private final AuthUserAccountPort authUserAccountPort;
	private final IssueJwtUseCase issueJwtUseCase;

	public LoginWithGoogleService(
			GoogleOAuthClient googleOAuthClient,
			AuthUserAccountPort authUserAccountPort,
			IssueJwtUseCase issueJwtUseCase) {
		this.googleOAuthClient = googleOAuthClient;
		this.authUserAccountPort = authUserAccountPort;
		this.issueJwtUseCase = issueJwtUseCase;
	}

	@Override
	@Transactional
	public JwtTokenResult login(String authorizationCode, String redirectUri) {
		GoogleUserProfile profile = googleOAuthClient.exchangeCode(authorizationCode, redirectUri);
		if (!profile.emailVerified()) {
			throw new AuthDomainException(
					HttpStatus.FORBIDDEN.value(),
					"Email Google non vérifié — connexion refusée");
		}
		if (profile.email() == null || profile.email().isBlank()) {
			throw new AuthDomainException(
					HttpStatus.FORBIDDEN.value(),
					"Email Google manquant — connexion refusée");
		}
		String email = profile.email().trim().toLowerCase();
		String displayName = DisplayNames.forProvider(profile.displayName(), email);

		AuthUserAccount account = authUserAccountPort
				.findByProviderSubject(PROVIDER_GOOGLE, profile.subject())
				.orElseGet(() -> resolveOrCreateAccount(email, displayName, profile));

		if (!account.active()) {
			throw new AuthDomainException(HttpStatus.FORBIDDEN.value(), "Compte désactivé");
		}
		return issueJwtUseCase.issue(account.id(), account.email());
	}

	private AuthUserAccount resolveOrCreateAccount(
			String email, String displayName, GoogleUserProfile profile) {
		return authUserAccountPort.findByEmail(email)
				.map(existing -> {
					authUserAccountPort.linkIdentity(
							existing.id(),
							PROVIDER_GOOGLE,
							profile.subject(),
							email);
					return existing;
				})
				.orElseGet(() -> authUserAccountPort.createWithIdentity(
						email,
						displayName,
						PROVIDER_GOOGLE,
						profile.subject(),
						email));
	}

}
