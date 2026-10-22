package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.appv4.auth.application.port.in.BuildAppleAuthorizeUrlUseCase;
import com.appv4.auth.application.port.in.LoginWithAppleUseCase;
import com.appv4.auth.application.result.JwtTokenResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Endpoints publics OAuth Apple (SCRUM-102).
 *
 * <p>Redirect FE typique : {@code http://localhost:4201/auth/callback/apple}.
 */
@RestController
@RequestMapping("/api/int/auth/oauth/apple")
public class AppleOAuthController {

	private final BuildAppleAuthorizeUrlUseCase buildAppleAuthorizeUrlUseCase;
	private final LoginWithAppleUseCase loginWithAppleUseCase;

	public AppleOAuthController(
			BuildAppleAuthorizeUrlUseCase buildAppleAuthorizeUrlUseCase,
			LoginWithAppleUseCase loginWithAppleUseCase) {
		this.buildAppleAuthorizeUrlUseCase = buildAppleAuthorizeUrlUseCase;
		this.loginWithAppleUseCase = loginWithAppleUseCase;
	}

	@GetMapping("/authorize-url")
	public ResponseEntity<AuthorizeUrlResponse> authorizeUrl(@RequestParam String redirectUri) {
		String url = buildAppleAuthorizeUrlUseCase.build(redirectUri);
		return ResponseEntity.ok(new AuthorizeUrlResponse(url));
	}

	@PostMapping
	public ResponseEntity<JwtTokenResponse> login(@Valid @RequestBody AppleLoginRequest request) {
		JwtTokenResult result = loginWithAppleUseCase.login(
				request.authorizationCode(),
				request.redirectUri());
		return ResponseEntity.ok(new JwtTokenResponse(result.token(), result.expiresAtEpochSeconds()));
	}

	public record AuthorizeUrlResponse(String authorizationUrl) {
	}

	public record AppleLoginRequest(
			@NotBlank String authorizationCode,
			@NotBlank String redirectUri) {
	}

	public record JwtTokenResponse(String token, long expiresAtEpochSeconds) {
	}
}
