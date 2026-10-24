package com.appv4.auth.application.service;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.in.LoginWithFacebookUseCase;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.FacebookOAuthClient;
import com.appv4.auth.application.port.out.FacebookOAuthClient.FacebookUserProfile;
import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Orchestration login Facebook : profil → upsert user/identity → JWT.
 *
 * <p><b>Métier / flow :</b> miroir de {@link LoginWithGoogleService} avec
 * {@code provider=facebook}.
 */
public class LoginWithFacebookService implements LoginWithFacebookUseCase {

	static final String PROVIDER_FACEBOOK = "facebook";

	private final FacebookOAuthClient facebookOAuthClient;
	private final AuthUserAccountPort authUserAccountPort;
	private final IssueJwtUseCase issueJwtUseCase;

	public LoginWithFacebookService(
			FacebookOAuthClient facebookOAuthClient,
			AuthUserAccountPort authUserAccountPort,
			IssueJwtUseCase issueJwtUseCase) {
		this.facebookOAuthClient = facebookOAuthClient;
		this.authUserAccountPort = authUserAccountPort;
		this.issueJwtUseCase = issueJwtUseCase;
	}

	@Override
	@Transactional
	public JwtTokenResult login(String authorizationCode, String redirectUri) {
		FacebookUserProfile profile = facebookOAuthClient.exchangeCode(authorizationCode, redirectUri);
		if (profile.email() == null || profile.email().isBlank()) {
			throw new AuthDomainException(
					HttpStatus.FORBIDDEN.value(),
					"Email Facebook manquant — connexion refusée (autorise le scope email)");
		}
		String email = profile.email().trim().toLowerCase();
		String displayName = DisplayNames.forProvider(profile.displayName(), email);

		AuthUserAccount account = authUserAccountPort
				.findByProviderSubject(PROVIDER_FACEBOOK, profile.subject())
				.orElseGet(() -> resolveOrCreateAccount(email, displayName, profile));

		if (!account.active()) {
			throw new AuthDomainException(HttpStatus.FORBIDDEN.value(), "Compte désactivé");
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
