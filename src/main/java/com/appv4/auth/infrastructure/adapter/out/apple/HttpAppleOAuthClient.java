package com.appv4.auth.infrastructure.adapter.out.apple;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.AppleOAuthClient;
import com.appv4.auth.infrastructure.config.AppleOAuthProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Adaptateur HTTP Sign in with Apple — token + decode {@code id_token} (SCRUM-102).
 *
 * <p><b>Technique :</b> échange form POST ; le profil (sub, email) est dans l'id_token
 * renvoyé par Apple (pas d'endpoint /me séparé).
 */
public class HttpAppleOAuthClient implements AppleOAuthClient {

	private final AppleOAuthProperties properties;
	private final RestClient restClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public HttpAppleOAuthClient(AppleOAuthProperties properties, RestClient.Builder restClientBuilder) {
		this.properties = properties;
		this.restClient = restClientBuilder.build();
	}

	@Override
	public AppleUserProfile exchangeCode(String authorizationCode, String redirectUri) {
		if (!properties.isConfigured()) {
			throw new AuthDomainException(
					HttpStatus.SERVICE_UNAVAILABLE.value(),
					"Apple OAuth non configuré (APPLE_CLIENT_ID / TEAM_ID / KEY_ID / PRIVATE_KEY)");
		}
		try {
			TokenResponse token = requestTokens(authorizationCode, redirectUri);
			if (token == null || token.idToken() == null || token.idToken().isBlank()) {
				throw new AuthDomainException(
						HttpStatus.UNAUTHORIZED.value(),
						"Réponse token Apple invalide (id_token manquant)");
			}
			return parseIdToken(token.idToken());
		} catch (RestClientException ex) {
			throw new AuthDomainException(
					HttpStatus.UNAUTHORIZED.value(),
					"Échec échange code Apple : " + ex.getMessage());
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

	/**
	 * Decode le payload JWT reçu directement de l'endpoint token Apple (TLS).
	 * Vérif JWKS complète = amélioration possible plus tard.
	 */
	private AppleUserProfile parseIdToken(String idToken) {
		try {
			String[] parts = idToken.split("\\.");
			if (parts.length < 2) {
				throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "id_token Apple malformé");
			}
			byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
			Map<String, Object> claims = objectMapper.readValue(
					new String(payloadBytes, StandardCharsets.UTF_8),
					new TypeReference<>() {
					});
			Object sub = claims.get("sub");
			if (sub == null || sub.toString().isBlank()) {
				throw new AuthDomainException(HttpStatus.UNAUTHORIZED.value(), "Profil Apple invalide (sub)");
			}
			Object emailObj = claims.get("email");
			String email = emailObj != null ? emailObj.toString() : null;
			return new AppleUserProfile(sub.toString(), email, "");
		} catch (AuthDomainException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new AuthDomainException(
					HttpStatus.UNAUTHORIZED.value(),
					"Impossible de lire id_token Apple : " + ex.getMessage());
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TokenResponse(@JsonProperty("id_token") String idToken) {
	}
}
