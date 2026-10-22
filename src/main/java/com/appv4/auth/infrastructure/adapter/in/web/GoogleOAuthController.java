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
import com.appv4.auth.application.result.JwtTokenResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Endpoints publics OAuth Google (SCRUM-96).
 *
 * <p>Flux SPA :
 * <ol>
 *   <li>FE appelle {@code GET .../authorize-url?redirectUri=...}</li>
 *   <li>Navigateur → Google → revient sur redirectUri avec {@code ?code=...}</li>
 *   <li>FE {@code POST .../google} avec code + même redirectUri → JWT</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/int/auth/oauth/google")
@Tag(name = "Auth — OAuth Google")
public class GoogleOAuthController {

	private final BuildGoogleAuthorizeUrlUseCase buildGoogleAuthorizeUrlUseCase;
	private final LoginWithGoogleUseCase loginWithGoogleUseCase;

	public GoogleOAuthController(
			BuildGoogleAuthorizeUrlUseCase buildGoogleAuthorizeUrlUseCase,
			LoginWithGoogleUseCase loginWithGoogleUseCase) {
		this.buildGoogleAuthorizeUrlUseCase = buildGoogleAuthorizeUrlUseCase;
		this.loginWithGoogleUseCase = loginWithGoogleUseCase;
	}

	@GetMapping("/authorize-url")
	@Operation(operationId = "getGoogleAuthorizeUrl", summary = "URL d'autorisation Google")
	public ResponseEntity<AuthorizeUrlResponse> authorizeUrl(@RequestParam String redirectUri) {
		String url = buildGoogleAuthorizeUrlUseCase.build(redirectUri);
		return ResponseEntity.ok(new AuthorizeUrlResponse(url));
	}

	@PostMapping
	@Operation(operationId = "loginWithGoogle", summary = "Échanger le code Google contre un JWT")
	public ResponseEntity<JwtTokenResponse> login(@Valid @RequestBody GoogleLoginRequest request) {
		JwtTokenResult result = loginWithGoogleUseCase.login(
				request.authorizationCode(),
				request.redirectUri());
		return ResponseEntity.ok(new JwtTokenResponse(result.token(), result.expiresAtEpochSeconds()));
	}

	@Schema(name = "AuthorizeUrlResponse")
	public record AuthorizeUrlResponse(String authorizationUrl) {
	}

	@Schema(name = "GoogleLoginRequest")
	public record GoogleLoginRequest(
			@NotBlank String authorizationCode,
			@NotBlank String redirectUri) {
	}

	@Schema(name = "JwtTokenResponse")
	public record JwtTokenResponse(String token, long expiresAtEpochSeconds) {
	}
}
