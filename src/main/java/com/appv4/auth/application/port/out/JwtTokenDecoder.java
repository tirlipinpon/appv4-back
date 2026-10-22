package com.appv4.auth.application.port.out;

import java.util.Optional;

/**
 * Port sortant — décodage / validation JWT.
 */
public interface JwtTokenDecoder {

	Optional<AuthenticatedUser> decode(String rawToken);

	/**
	 * Identité extraite d'un JWT valide.
	 */
	record AuthenticatedUser(long userId, String email) {
	}
}
