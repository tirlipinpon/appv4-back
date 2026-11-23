package com.appv4.auth.infrastructure.adapter.in.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.BuildAppleAuthorizeUrlUseCase;
import com.appv4.auth.application.port.in.LoginWithAppleUseCase;
import com.appv4.auth.application.port.in.ResumeAppleOAuthCallbackUseCase;
import com.appv4.auth.application.port.out.OAuthBrowserBindingPort;
import com.appv4.auth.application.result.JwtTokenResult;
import com.appv4.auth.infrastructure.config.AppleOAuthProperties;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Endpoints publics OAuth Apple (SCRUM-102 / issues #33 + #32).
 */
@RestController
@RequestMapping("/api/int/auth/oauth/apple")
@Tag(name = "Auth — OAuth Apple")
public class AppleOAuthController {

	private final BuildAppleAuthorizeUrlUseCase buildAppleAuthorizeUrlUseCase;
	private final ResumeAppleOAuthCallbackUseCase resumeAppleOAuthCallbackUseCase;
	private final LoginWithAppleUseCase loginWithAppleUseCase;
	private final AppleOAuthProperties appleOAuthProperties;
	private final OAuthBrowserBindingPort oauthBrowserBindingPort;

	public AppleOAuthController(
			BuildAppleAuthorizeUrlUseCase buildAppleAuthorizeUrlUseCase,
			ResumeAppleOAuthCallbackUseCase resumeAppleOAuthCallbackUseCase,
			LoginWithAppleUseCase loginWithAppleUseCase,
			AppleOAuthProperties appleOAuthProperties,
			OAuthBrowserBindingPort oauthBrowserBindingPort) {
		this.buildAppleAuthorizeUrlUseCase = buildAppleAuthorizeUrlUseCase;
		this.resumeAppleOAuthCallbackUseCase = resumeAppleOAuthCallbackUseCase;
		this.loginWithAppleUseCase = loginWithAppleUseCase;
		this.appleOAuthProperties = appleOAuthProperties;
		this.oauthBrowserBindingPort = oauthBrowserBindingPort;
	}

	@GetMapping("/authorize-url")
	@Operation(
			operationId = "getAppleAuthorizeUrl",
			summary = "URL d'autorisation Apple (form_post vers le callback back)")
	public ResponseEntity<AuthorizeUrlResponse> getAppleAuthorizeUrl(
			@RequestParam String redirectUri) {
		String state = oauthBrowserBindingPort.issue(
				OAuthBrowserBindingPort.PROVIDER_APPLE, redirectUri);
		String url = buildAppleAuthorizeUrlUseCase.build(redirectUri, state);
		return ResponseEntity.ok(new AuthorizeUrlResponse(url, state));
	}

	@PostMapping(value = "/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	@Operation(operationId = "appleOAuthFormPostCallback", summary = "Callback form_post Apple → redirect SPA")
	public ResponseEntity<Void> appleFormPostCallback(
			@RequestParam(required = false) String state,
			@RequestParam(required = false) String code,
			@RequestParam(required = false) String error) {
		URI location = resumeAppleOAuthCallbackUseCase.resume(state, code, error);
		return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
	}

	@PostMapping
	@Operation(
			operationId = "loginWithApple",
			summary = "Échanger le code Apple contre un JWT",
			description = "redirectUri = URL SPA de reprise ; l'échange token utilise APPLE_CALLBACK_URI.")
	public ResponseEntity<JwtTokenResponse> loginWithApple(@Valid @RequestBody AppleLoginRequest request) {
		oauthBrowserBindingPort.consume(
				OAuthBrowserBindingPort.PROVIDER_APPLE,
				request.redirectUri(),
				request.state());
		String tokenRedirect = appleOAuthProperties.isCallbackConfigured()
				? appleOAuthProperties.getCallbackUri().trim()
				: request.redirectUri();
		JwtTokenResult result = loginWithAppleUseCase.login(
				request.authorizationCode(),
				tokenRedirect);
		return ResponseEntity.ok(new JwtTokenResponse(result.token(), result.expiresAtEpochSeconds()));
	}

	@Schema(name = "AppleLoginRequest")
	public record AppleLoginRequest(
			@NotBlank String authorizationCode,
			@NotBlank @Schema(description = "URL SPA de reprise (lié au state)") String redirectUri,
			@NotBlank String state) {
	}
}
