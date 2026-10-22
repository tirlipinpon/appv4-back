package com.appv4.auth.application.port.out;

import java.util.Optional;

/**
 * Port sortant — lire / valider un JWT reçu du client.
 *
 * <p><b>Métier :</b> « ce Bearer est-il encore valide ? qui est-ce ? »
 * Si oui → {@link AuthenticatedUser} ; sinon → {@link Optional#empty()}
 * (filtre = anonyme → 401 sur /api/int).
 *
 * <p><b>Technique :</b> vérifie signature (secret) + expiration. Pas d'accès DB ici.
 */
public interface JwtTokenDecoder {

	Optional<AuthenticatedUser> decode(String rawToken);

	/**
	 * Identité extraite d'un JWT valide (claims sub / email).
	 * Les rôles métier (parent/teacher/admin) arriveront plus tard.
	 */
	record AuthenticatedUser(long userId, String email) {
	}
}
