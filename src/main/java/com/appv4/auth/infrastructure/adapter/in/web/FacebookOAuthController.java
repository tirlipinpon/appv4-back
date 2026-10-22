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
import com.appv4.auth.application.result.JwtTokenResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Endpoints publics OAuth Facebook (SCRUM-99).
 *
 * <p><b>Business :</b> même flux SPA que Google — bouton FE → Facebook → callback → JWT.
 * <p><b>Technique :</b> {@code GET .../authorize-url} + {@code POST .../facebook}.
 * Redirect FE typique : {@code http://localhost:4201/auth/callback/facebook}.
 */
@RestController
@RequestMapping("/api/int/auth/oauth/facebook")
@Tag(name = "Auth — OAuth Facebook")
public class FacebookOAuthController {

	private final BuildFacebookAuthorizeUrlUseCase buildFacebookAuthorizeUrlUseCase;
	private final LoginWithFacebookUseCase loginWithFacebookUseCase;

	public FacebookOAuthController(
			BuildFacebookAuthorizeUrlUseCase buildFacebookAuthorizeUrlUseCase,
			LoginWithFacebookUseCase loginWithFacebookUseCase) {
		this.buildFacebookAuthorizeUrlUseCase = buildFacebookAuthorizeUrlUseCase;
		this.loginWithFacebookUseCase = loginWithFacebookUseCase;
	}

	@GetMapping("/authorize-url")
	@Operation(operationId = "getFacebookAuthorizeUrl", summary = "URL d'autorisation Facebook")
	public ResponseEntity<AuthorizeUrlResponse> authorizeUrl(@RequestParam String redirectUri) {
		String url = buildFacebookAuthorizeUrlUseCase.build(redirectUri);
		return ResponseEntity.ok(new AuthorizeUrlResponse(url));
	}

	@PostMapping
	@Operation(operationId = "loginWithFacebook", summary = "Échanger le code Facebook contre un JWT")
	public ResponseEntity<JwtTokenResponse> login(@Valid @RequestBody FacebookLoginRequest request) {
		JwtTokenResult result = loginWithFacebookUseCase.login(
				request.authorizationCode(),
				request.redirectUri());
		return ResponseEntity.ok(new JwtTokenResponse(result.token(), result.expiresAtEpochSeconds()));
	}

	@Schema(name = "AuthorizeUrlResponse")
	public record AuthorizeUrlResponse(String authorizationUrl) {
	}

	@Schema(name = "FacebookLoginRequest")
	public record FacebookLoginRequest(
			@NotBlank String authorizationCode,
			@NotBlank String redirectUri) {
	}

	@Schema(name = "JwtTokenResponse")
	public record JwtTokenResponse(String token, long expiresAtEpochSeconds) {
	}
}
