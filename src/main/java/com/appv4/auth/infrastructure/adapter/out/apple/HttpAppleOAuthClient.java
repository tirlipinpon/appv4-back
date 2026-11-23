package com.appv4.auth.infrastructure.adapter.out.apple;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.AppleOAuthClient;
import com.appv4.auth.infrastructure.adapter.out.oauth.OAuthRestClientErrors;
import com.appv4.auth.infrastructure.config.AppleOAuthProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.jsonwebtoken.Claims;

/**
 * Adaptateur HTTP Sign in with Apple — token + validation OIDC {@code id_token} (SCRUM-102 / #52).
 *
 * <p><b>Technique :</b> échange form POST ; profil (sub, email) depuis id_token après
 * vérif JWKS / iss / aud / exp.
 */
public class HttpAppleOAuthClient implements AppleOAuthClient {

	private final AppleOAuthProperties properties;
	private final RestClient restClient;
	private final AppleIdTokenValidator idTokenValidator;

	public HttpAppleOAuthClient(AppleOAuthProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder.build();
		this.idTokenValidator = new AppleIdTokenValidator(restClientBuilder, properties.getJwksUri());
	}

	HttpAppleOAuthClient(
			AppleOAuthProperties properties,
			RestClient.Builder restClientBuilder,
			AppleIdTokenValidator idTokenValidator) {
		this.properties = properties;
		this.restClient = restClientBuilder.build();
		this.idTokenValidator = idTokenValidator;
	}

	@Override
	public AppleUserProfile exchangeCode(String authorizationCode, String redirectUri) {
		if (!properties.isConfigured()) {
			throw new AuthDomainException(AuthErrorCode.SERVICE_UNAVAILABLE,
					"Apple OAuth non configuré (APPLE_CLIENT_ID / TEAM_ID / KEY_ID / PRIVATE_KEY)");
		}
		try {
			TokenResponse token = requestTokens(authorizationCode, redirectUri);
			if (token == null || token.idToken() == null || token.idToken().isBlank()) {
				throw new AuthDomainException(AuthErrorCode.UNAUTHORIZED,
						"Réponse token Apple invalide (id_token manquant)");
			}
			return toProfile(idTokenValidator.validate(token.idToken(), properties.getClientId()));
		} catch (RestClientException ex) {
			throw OAuthRestClientErrors.map(ex, "Apple");
		}
	}

	private TokenResponse requestTokens(String authorizationCode, String redirectUri) {
		String clientSecret = AppleClientSecretGenerator.generate(properties);
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("client_id", properties.getClientId());
		form.add("client_secret", clientSecret);
		form.add("code", authorizationCode);
		form.add("grant_type", "authorization_code");
		form.add("redirect_uri", redirectUri);

		return restClient.post()
				.uri(properties.getTokenUri())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(TokenResponse.class);
	}

	static AppleUserProfile toProfile(Claims claims) {
		String sub = claims.getSubject();
		if (sub == null || sub.isBlank()) {
			throw new AuthDomainException(AuthErrorCode.UNAUTHORIZED, "Profil Apple invalide (sub)");
		}
		String email = null;
		Object emailObj = claims.get("email");
		if (emailObj != null && isEmailVerified(claims.get("email_verified"))) {
			email = emailObj.toString();
		}
		return new AppleUserProfile(sub, email, "");
	}

	static boolean isEmailVerified(Object emailVerified) {
		if (emailVerified == null) {
			return false;
		}
		if (emailVerified instanceof Boolean bool) {
			return bool;
		}
		return "true".equalsIgnoreCase(emailVerified.toString());
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TokenResponse(@JsonProperty("id_token") String idToken) {
	}
}
