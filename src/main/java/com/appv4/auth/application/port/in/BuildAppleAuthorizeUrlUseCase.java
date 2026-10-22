package com.appv4.auth.application.port.in;

/**
 * Port in — URL d'autorisation Apple (Services ID + redirect FE).
 */
public interface BuildAppleAuthorizeUrlUseCase {

	String build(String redirectUri);
}
