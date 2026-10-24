package com.appv4.auth.application.service;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.in.LoginWithGoogleUseCase;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.GoogleOAuthClient;
import com.appv4.auth.application.port.out.GoogleOAuthClient.GoogleUserProfile;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Orchestration login Google : profil (réseau) puis upsert + JWT (TX courte).
 *
 * <p>L'échange OAuth est hors transaction (issue #39).
 */
public class LoginWithGoogleService implements LoginWithGoogleUseCase {

	static final String PROVIDER_GOOGLE = "google";

	private final GoogleOAuthClient googleOAuthClient;
	private final AuthUserAccountPort authUserAccountPort;
	private final IssueJwtUseCase issueJwtUseCase;
	private final TransactionTemplate transactionTemplate;

	public LoginWithGoogleService(
			GoogleOAuthClient googleOAuthClient,
			AuthUserAccountPort authUserAccountPort,
			IssueJwtUseCase issueJwtUseCase,
			TransactionTemplate transactionTemplate) {
		this.googleOAuthClient = googleOAuthClient;
		this.authUserAccountPort = authUserAccountPort;
		this.issueJwtUseCase = issueJwtUseCase;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public JwtTokenResult login(String authorizationCode, String redirectUri) {
		GoogleUserProfile profile = googleOAuthClient.exchangeCode(authorizationCode, redirectUri);
		if (!profile.emailVerified()) {
			throw new AuthDomainException(
					HttpStatus.FORBIDDEN.value(),
					"Email Google non vérifié — connexion refusée");
		}
		String email = Emails.requireNormalizedOrThrow(
				profile.email(), "Email Google manquant — connexion refusée");
		String displayName = DisplayNames.forProvider(profile.displayName(), email);

		return transactionTemplate.execute(status -> persistAndIssue(profile, email, displayName));
	}

	private JwtTokenResult persistAndIssue(GoogleUserProfile profile, String email, String displayName) {
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
				.map(existing -> linkVerifiedSocial(existing, PROVIDER_GOOGLE, profile.subject(), email))
				.orElseGet(() -> authUserAccountPort.createWithIdentity(
						email,
						displayName,
						PROVIDER_GOOGLE,
						profile.subject(),
						email));
	}

	private AuthUserAccount linkVerifiedSocial(
			AuthUserAccount existing, String provider, String subject, String emailAtProvider) {
		authUserAccountPort.linkIdentity(existing.id(), provider, subject, emailAtProvider);
		authUserAccountPort.clearPasswordAfterVerifiedSocialLink(existing.id());
		return existing;
	}
}
