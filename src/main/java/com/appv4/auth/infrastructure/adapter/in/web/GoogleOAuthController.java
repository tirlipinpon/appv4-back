package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.BuildGoogleAuthorizeUrlUseCase;
import com.appv4.auth.application.port.in.LoginWithGoogleUseCase;
import com.appv4.auth.application.port.out.OAuthBrowserBindingPort;
import com.appv4.auth.application.result.JwtTokenResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Endpoints publics OAuth Google (SCRUM-96 / issue #32).
 */
@RestController
@RequestMapping("/api/int/auth/oauth/google")
@Tag(name = "Auth — OAuth Google")
public class GoogleOAuthController {

	private final BuildGoogleAuthorizeUrlUseCase buildGoogleAuthorizeUrlUseCase;
	private final LoginWithGoogleUseCase loginWithGoogleUseCase;
	private final OAuthBrowserBindingPort oauthBrowserBindingPort;

	public GoogleOAuthController(
			BuildGoogleAuthorizeUrlUseCase buildGoogleAuthorizeUrlUseCase,
			LoginWithGoogleUseCase loginWithGoogleUseCase,
			OAuthBrowserBindingPort oauthBrowserBindingPort) {
		this.buildGoogleAuthorizeUrlUseCase = buildGoogleAuthorizeUrlUseCase;
		this.loginWithGoogleUseCase = loginWithGoogleUseCase;
		this.oauthBrowserBindingPort = oauthBrowserBindingPort;
	}

	@GetMapping("/authorize-url")
	@Operation(operationId = "getGoogleAuthorizeUrl", summary = "URL d'autorisation Google")
	public ResponseEntity<AuthorizeUrlResponse> getGoogleAuthorizeUrl(@RequestParam String redirectUri) {
		String state = oauthBrowserBindingPort.issue(OAuthBrowserBindingPort.PROVIDER_GOOGLE, redirectUri);
		String url = buildGoogleAuthorizeUrlUseCase.build(redirectUri, state);
		return ResponseEntity.ok(new AuthorizeUrlResponse(url, state));
	}

	@PostMapping
	@Operation(operationId = "loginWithGoogle", summary = "Échanger le code Google contre un JWT")
	public ResponseEntity<JwtTokenResponse> loginWithGoogle(
			@Valid @RequestBody GoogleLoginRequest request) {
		oauthBrowserBindingPort.consume(
				OAuthBrowserBindingPort.PROVIDER_GOOGLE,
				request.redirectUri(),
				request.state());
		JwtTokenResult result = loginWithGoogleUseCase.login(
				request.authorizationCode(),
				request.redirectUri());
		return ResponseEntity.ok(new JwtTokenResponse(result.token(), result.expiresAtEpochSeconds()));
	}

	@Schema(name = "GoogleLoginRequest")
	public record GoogleLoginRequest(
			@NotBlank String authorizationCode,
			@NotBlank String redirectUri,
			@NotBlank String state) {
	}
}
