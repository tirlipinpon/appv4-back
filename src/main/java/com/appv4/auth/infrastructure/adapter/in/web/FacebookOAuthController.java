package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.BuildFacebookAuthorizeUrlUseCase;
import com.appv4.auth.application.port.in.LoginWithFacebookUseCase;
import com.appv4.auth.application.port.out.OAuthBrowserBindingPort;
import com.appv4.auth.application.result.JwtTokenResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Endpoints publics OAuth Facebook (SCRUM-99 / issue #32).
 */
@RestController
@RequestMapping("/api/int/auth/oauth/facebook")
@Tag(name = "Auth — OAuth Facebook")
public class FacebookOAuthController {

	private final BuildFacebookAuthorizeUrlUseCase buildFacebookAuthorizeUrlUseCase;
	private final LoginWithFacebookUseCase loginWithFacebookUseCase;
	private final OAuthBrowserBindingPort oauthBrowserBindingPort;

	public FacebookOAuthController(
			BuildFacebookAuthorizeUrlUseCase buildFacebookAuthorizeUrlUseCase,
			LoginWithFacebookUseCase loginWithFacebookUseCase,
			OAuthBrowserBindingPort oauthBrowserBindingPort) {
		this.buildFacebookAuthorizeUrlUseCase = buildFacebookAuthorizeUrlUseCase;
		this.loginWithFacebookUseCase = loginWithFacebookUseCase;
		this.oauthBrowserBindingPort = oauthBrowserBindingPort;
	}

	@GetMapping("/authorize-url")
	@Operation(operationId = "getFacebookAuthorizeUrl", summary = "URL d'autorisation Facebook")
	public ResponseEntity<AuthorizeUrlResponse> getFacebookAuthorizeUrl(
			@RequestParam String redirectUri) {
		String state = oauthBrowserBindingPort.issue(OAuthBrowserBindingPort.PROVIDER_FACEBOOK, redirectUri);
		String url = buildFacebookAuthorizeUrlUseCase.build(redirectUri, state);
		return ResponseEntity.ok(new AuthorizeUrlResponse(url, state));
	}

	@PostMapping
	@Operation(operationId = "loginWithFacebook", summary = "Échanger le code Facebook contre un JWT")
	public ResponseEntity<JwtTokenResponse> loginWithFacebook(
			@Valid @RequestBody FacebookLoginRequest request) {
		oauthBrowserBindingPort.consume(
				OAuthBrowserBindingPort.PROVIDER_FACEBOOK,
				request.redirectUri(),
				request.state());
		JwtTokenResult result = loginWithFacebookUseCase.login(
				request.authorizationCode(),
				request.redirectUri());
		return ResponseEntity.ok(new JwtTokenResponse(result.token(), result.expiresAtEpochSeconds()));
	}

	@Schema(name = "FacebookLoginRequest")
	public record FacebookLoginRequest(
			@NotBlank String authorizationCode,
			@NotBlank String redirectUri,
			@NotBlank String state) {
	}
}
