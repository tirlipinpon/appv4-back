package com.appv4.auth.infrastructure.adapter.out.google;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.GoogleOAuthClient;
import com.appv4.auth.infrastructure.config.GoogleOAuthProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Adaptateur HTTP Google — token endpoint + userinfo (SCRUM-96).
 */
public class HttpGoogleOAuthClient implements GoogleOAuthClient {

	private final GoogleOAuthProperties properties;
	private final RestClient restClient;

	public HttpGoogleOAuthClient(GoogleOAuthProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder.build();
	}

	@Override
	public GoogleUserProfile exchangeCode(String authorizationCode, String redirectUri) {
		if (!properties.isConfigured()) {
			throw new AuthDomainException(
					HttpStatus.SERVICE_UNAVAILABLE.value(),
					"Google OAuth non configuré (GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET)");
		}
		try {
			String accessToken = requestAccessToken(authorizationCode, redirectUri);
			return fetchUserInfo(accessToken);
		} catch (RestClientException ex) {
			throw new AuthDomainException(
					HttpStatus.UNAUTHORIZED.value(),
					"Échec échange code Google : " + ex.getMessage());
		}
	}

	private String requestAccessToken(String authorizationCode, String redirectUri) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("code", authorizationCode);
		form.add("client_id", properties.getClientId());
		form.add("client_secret", properties.getClientSecret());
		form.add("redirect_uri", redirectUri);
		form.add("grant_type", "authorization_code");

		TokenResponse token = restClient.post()
				.uri(properties.getTokenUri())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(TokenResponse.class);

		if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
			throw new AuthDomainException(
					HttpStatus.UNAUTHORIZED.value(),
					"Réponse token Google invalide");
		}
		return token.accessToken();
	}

	private GoogleUserProfile fetchUserInfo(String accessToken) {
		UserInfoResponse info = restClient.get()
				.uri(properties.getUserInfoUri())
				.headers(h -> h.setBearerAuth(accessToken))
				.retrieve()
				.body(UserInfoResponse.class);

		if (info == null || info.sub() == null || info.sub().isBlank()) {
			throw new AuthDomainException(
					HttpStatus.UNAUTHORIZED.value(),
					"Profil Google invalide");
		}
		boolean verified = Boolean.TRUE.equals(info.emailVerified());
		String name = info.name() != null ? info.name() : "";
		return new GoogleUserProfile(info.sub(), info.email(), verified, name);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TokenResponse(@JsonProperty("access_token") String accessToken) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record UserInfoResponse(
			String sub,
			String email,
			@JsonProperty("email_verified") Boolean emailVerified,
			String name) {
	}
}
