package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.JwtTokenResult;

/**
 * Port in — login Apple (code → JWT appv4).
 */
public interface LoginWithAppleUseCase {

	JwtTokenResult login(String authorizationCode, String redirectUri);
}
