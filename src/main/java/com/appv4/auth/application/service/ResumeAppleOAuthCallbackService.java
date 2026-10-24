package com.appv4.auth.application.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.springframework.http.HttpStatus;

import com.appv4.auth.application.config.AppleOAuthSettings;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.ResumeAppleOAuthCallbackUseCase;
import com.appv4.auth.application.port.out.OAuthBrowserBindingPort;

/**
 * Après le {@code form_post} Apple : redirige le navigateur vers le SPA avec le code.
 */
public class ResumeAppleOAuthCallbackService implements ResumeAppleOAuthCallbackUseCase {

	private final Supplier<AppleOAuthSettings> settings;
	private final OAuthBrowserBindingPort oauthBrowserBindingPort;

	public ResumeAppleOAuthCallbackService(
			Supplier<AppleOAuthSettings> settings,
			OAuthBrowserBindingPort oauthBrowserBindingPort) {
		this.settings = settings;
		this.oauthBrowserBindingPort = oauthBrowserBindingPort;
	}

	public ResumeAppleOAuthCallbackService(
			AppleOAuthSettings settings,
			OAuthBrowserBindingPort oauthBrowserBindingPort) {
		this(() -> settings, oauthBrowserBindingPort);
	}

	@Override
	public URI resume(String state, String code, String error) {
		AppleOAuthSettings s = settings.get();
		String resumeUri = oauthBrowserBindingPort.peek(OAuthBrowserBindingPort.PROVIDER_APPLE, state);
		if (!AppleOAuthReturnUris.isAllowed(resumeUri, s.allowedReturnUriPrefixes())) {
			throw new AuthDomainException(
					HttpStatus.BAD_REQUEST.value(),
					"URL de reprise Apple non autorisée");
		}
		StringBuilder target = new StringBuilder(resumeUri);
		target.append(resumeUri.contains("?") ? '&' : '?');
		if (error != null && !error.isBlank()) {
			target.append("error=")
					.append(URLEncoder.encode(error, StandardCharsets.UTF_8));
			return URI.create(target.toString());
		}
		if (code == null || code.isBlank()) {
			throw new AuthDomainException(HttpStatus.BAD_REQUEST.value(), "code Apple manquant");
		}
		target.append("code=")
				.append(URLEncoder.encode(code, StandardCharsets.UTF_8));
		target.append("&state=")
				.append(URLEncoder.encode(state, StandardCharsets.UTF_8));
		return URI.create(target.toString());
	}
}
