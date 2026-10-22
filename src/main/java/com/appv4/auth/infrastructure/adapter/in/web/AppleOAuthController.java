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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Endpoints publics OAuth Apple (SCRUM-102).
 *
 * <p>Redirect FE typique : {@code http://localhost:4201/auth/callback/apple}.
 */
@RestController
@RequestMapping("/api/int/auth/oauth/apple")
@Tag(name = "Auth — OAuth Apple")
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
	@Operation(summary = "URL d'autorisation Apple")
	public ResponseEntity<AuthorizeUrlResponse> authorizeUrl(@RequestParam String redirectUri) {
		String url = buildAppleAuthorizeUrlUseCase.build(redirectUri);
		return ResponseEntity.ok(new AuthorizeUrlResponse(url));
	}

	@PostMapping
	@Operation(summary = "Échanger le code Apple contre un JWT")
	public ResponseEntity<JwtTokenResponse> login(@Valid @RequestBody AppleLoginRequest request) {
		JwtTokenResult result = loginWithAppleUseCase.login(
				request.authorizationCode(),
				request.redirectUri());
		return ResponseEntity.ok(new JwtTokenResponse(result.token(), result.expiresAtEpochSeconds()));
	}

	@Schema(name = "AuthorizeUrlResponse")
	public record AuthorizeUrlResponse(String authorizationUrl) {
	}

	@Schema(name = "AppleLoginRequest")
	public record AppleLoginRequest(
			@NotBlank String authorizationCode,
			@NotBlank String redirectUri) {
	}

	@Schema(name = "JwtTokenResponse")
	public record JwtTokenResponse(String token, long expiresAtEpochSeconds) {
	}
}
