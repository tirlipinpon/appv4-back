package com.appv4.auth.application.service;

import org.springframework.transaction.support.TransactionTemplate;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.in.LoginWithFacebookUseCase;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.FacebookOAuthClient;
import com.appv4.auth.application.port.out.FacebookOAuthClient.FacebookUserProfile;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Orchestration login Facebook : profil (réseau) puis upsert + JWT (TX courte).
 *
 * <p>L'échange OAuth est hors transaction (issue #39).
 */
public class LoginWithFacebookService implements LoginWithFacebookUseCase {

	static final String PROVIDER_FACEBOOK = "facebook";

	private final FacebookOAuthClient facebookOAuthClient;
	private final AuthUserAccountPort authUserAccountPort;
	private final IssueJwtUseCase issueJwtUseCase;
	private final TransactionTemplate transactionTemplate;

	public LoginWithFacebookService(
			FacebookOAuthClient facebookOAuthClient,
			AuthUserAccountPort authUserAccountPort,
			IssueJwtUseCase issueJwtUseCase,
			TransactionTemplate transactionTemplate) {
		this.facebookOAuthClient = facebookOAuthClient;
		this.authUserAccountPort = authUserAccountPort;
		this.issueJwtUseCase = issueJwtUseCase;
		this.transactionTemplate = transactionTemplate;
	}

	@Override
	public JwtTokenResult login(String authorizationCode, String redirectUri) {
		FacebookUserProfile profile = facebookOAuthClient.exchangeCode(authorizationCode, redirectUri);
		String email = Emails.requireNormalizedOrThrow(
				profile.email(),
				"Email Facebook manquant — connexion refusée (autorise le scope email)");
		String displayName = DisplayNames.forProvider(profile.displayName(), email);

		return transactionTemplate.execute(status -> persistAndIssue(profile, email, displayName));
	}

	private JwtTokenResult persistAndIssue(FacebookUserProfile profile, String email, String displayName) {
		AuthUserAccount account = authUserAccountPort
				.findByProviderSubject(PROVIDER_FACEBOOK, profile.subject())
				.orElseGet(() -> resolveOrCreateAccount(email, displayName, profile));

		if (!account.active()) {
			throw new AuthDomainException(AuthErrorCode.FORBIDDEN, "Compte désactivé");
		}
		return issueJwtUseCase.issue(account.id(), account.email());
	}

	private AuthUserAccount resolveOrCreateAccount(
			String email, String displayName, FacebookUserProfile profile) {
		return authUserAccountPort.findByEmail(email)
				.map(existing -> linkVerifiedSocial(existing, PROVIDER_FACEBOOK, profile.subject(), email))
				.orElseGet(() -> authUserAccountPort.createWithIdentity(
						email,
						displayName,
						PROVIDER_FACEBOOK,
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
