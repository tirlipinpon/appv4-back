package com.appv4.auth.application.service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.ResumeAppleOAuthCallbackUseCase;
import com.appv4.auth.application.port.out.OAuthBrowserBindingPort;
import com.appv4.auth.infrastructure.config.AppleOAuthProperties;

/**
 * Après le {@code form_post} Apple : redirige le navigateur vers le SPA avec le code (issue #33).
 *
 * <p><b>Business :</b> le SPA ne peut pas recevoir le POST Apple ; le back relaie en GET.
 * <p><b>Technique :</b> {@link OAuthBrowserBindingPort#peek} pour la reprise FE (issue #32).
 * <p><b>Métier / flow :</b> adapter in web callback → ce use case → 302 FE → échange JWT.
 */
public class ResumeAppleOAuthCallbackService implements ResumeAppleOAuthCallbackUseCase {

	private final AppleOAuthProperties properties;
	private final OAuthBrowserBindingPort oauthBrowserBindingPort;

	public ResumeAppleOAuthCallbackService(
			AppleOAuthProperties properties,
			OAuthBrowserBindingPort oauthBrowserBindingPort) {
		this.properties = properties;
		this.oauthBrowserBindingPort = oauthBrowserBindingPort;
	}

	@Override
	public URI resume(String state, String code, String error) {
		String resumeUri = oauthBrowserBindingPort.peek(OAuthBrowserBindingPort.PROVIDER_APPLE, state);
		if (!AppleOAuthReturnUris.isAllowed(resumeUri, properties.getAllowedReturnUriPrefixes())) {
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
