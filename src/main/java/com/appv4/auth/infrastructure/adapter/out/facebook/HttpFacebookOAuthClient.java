package com.appv4.auth.infrastructure.adapter.out.facebook;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.FacebookOAuthClient;
import com.appv4.auth.infrastructure.config.FacebookOAuthProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Adaptateur HTTP Facebook Graph — token + {@code /me} (SCRUM-99).
 *
 * <p><b>Technique :</b> Facebook échange le code via GET (query params), pas un form POST.
 */
public class HttpFacebookOAuthClient implements FacebookOAuthClient {

	private final FacebookOAuthProperties properties;
	private final RestClient restClient;

	public HttpFacebookOAuthClient(FacebookOAuthProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder.build();
	}

	@Override
	public FacebookUserProfile exchangeCode(String authorizationCode, String redirectUri) {
		if (!properties.isConfigured()) {
			throw new AuthDomainException(
					HttpStatus.SERVICE_UNAVAILABLE.value(),
					"Facebook OAuth non configuré (FACEBOOK_APP_ID / FACEBOOK_APP_SECRET)");
		}
		try {
			String accessToken = requestAccessToken(authorizationCode, redirectUri);
			return fetchUserInfo(accessToken);
		} catch (RestClientException ex) {
			throw new AuthDomainException(
					HttpStatus.UNAUTHORIZED.value(),
					"Échec échange code Facebook : " + ex.getMessage());
		}
	}

	private String requestAccessToken(String authorizationCode, String redirectUri) {
		URI uri = UriComponentsBuilder.fromUriString(properties.getTokenUri())
				.queryParam("client_id", properties.getAppId())
				.queryParam("client_secret", properties.getAppSecret())
				.queryParam("redirect_uri", redirectUri)
				.queryParam("code", authorizationCode)
				.build()
				.toUri();

		TokenResponse token = restClient.get()
				.uri(uri)
				.retrieve()
				.body(TokenResponse.class);

		if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
			throw new AuthDomainException(
					HttpStatus.UNAUTHORIZED.value(),
					"Réponse token Facebook invalide");
		}
		return token.accessToken();
	}

	private FacebookUserProfile fetchUserInfo(String accessToken) {
		URI uri = UriComponentsBuilder.fromUriString(properties.getUserInfoUri())
				.queryParam("fields", "id,name,email")
				.queryParam("access_token", accessToken)
				.build()
				.toUri();

		UserInfoResponse info = restClient.get()
				.uri(uri)
				.retrieve()
				.body(UserInfoResponse.class);

		if (info == null || info.id() == null || info.id().isBlank()) {
			throw new AuthDomainException(
					HttpStatus.UNAUTHORIZED.value(),
					"Profil Facebook invalide");
		}
		String name = info.name() != null ? info.name() : "";
		return new FacebookUserProfile(info.id(), info.email(), name);
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TokenResponse(@JsonProperty("access_token") String accessToken) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record UserInfoResponse(String id, String email, String name) {
	}
}
