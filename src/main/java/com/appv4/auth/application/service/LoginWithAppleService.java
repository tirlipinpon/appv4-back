package com.appv4.auth.application.service;

import org.springframework.transaction.support.TransactionTemplate;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.in.LoginWithAppleUseCase;
import com.appv4.auth.application.port.out.AppleOAuthClient;
import com.appv4.auth.application.port.out.AppleOAuthClient.AppleUserProfile;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Orchestration login Apple : profil (réseau) puis upsert + JWT (TX courte).
 *
 * <p>L'échange OAuth est hors transaction (issue #39). Sans email (re-login),
 * on retrouve uniquement via {@code provider_subject}.
 */
public class LoginWithAppleService implements LoginWithAppleUseCase {

	static final String PROVIDER_APPLE = "apple";

	private final AppleOAuthClient appleOAuthClient;
	private final AuthUserAccountPort authUserAccountPort;
	private final IssueJwtUseCase issueJwtUseCase;
	private final TransactionTemplate transactionTemplate;

	public LoginWithAppleService(
			AppleOAuthClient appleOAuthClient,
			AuthUserAccountPort authUserAccountPort,
			IssueJwtUseCase issueJwtUseCase,
			TransactionTemplate transactionTemplate) {
		this.appleOAuthClient = appleOAuthClient;
		this.authUserAccountPort = authUserAccountPort;
		this.issueJwtUseCase = issueJwtUseCase;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public JwtTokenResult login(String authorizationCode, String redirectUri) {
		AppleUserProfile profile = appleOAuthClient.exchangeCode(authorizationCode, redirectUri);
		return transactionTemplate.execute(status -> persistAndIssue(profile));
	}

	private JwtTokenResult persistAndIssue(AppleUserProfile profile) {
		AuthUserAccount account = authUserAccountPort
				.findByProviderSubject(PROVIDER_APPLE, profile.subject())
				.orElseGet(() -> createFromFirstConsent(profile));

		if (!account.active()) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, "Compte désactivé");
		}
		return issueJwtUseCase.issue(account.id(), account.email());
	}

	private AuthUserAccount createFromFirstConsent(AppleUserProfile profile) {
		String email = Emails.requireNormalizedOrThrow(
				profile.email(),
				"Email Apple manquant — 1er consentement requis (scope email)");
		String displayName = DisplayNames.forProvider(profile.displayName(), email);

		return authUserAccountPort.findByEmail(email)
				.map(existing -> linkVerifiedSocial(existing, PROVIDER_APPLE, profile.subject(), email))
				.orElseGet(() -> authUserAccountPort.createWithIdentity(
						email,
						displayName,
						PROVIDER_APPLE,
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
