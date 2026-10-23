package com.appv4.auth.application.port.out;

import java.util.List;
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
	 * Identité extraite d'un JWT valide.
	 *
	 * <p>{@code activeRole} est {@code null} quand le jeton ne choisit pas d'espace
	 * (deux rôles sans souvenir, ou ancien jeton). {@code roles} liste les espaces
	 * déjà accordés ; vide si le claim est absent.
	 */
	record AuthenticatedUser(
			long userId,
			String email,
			String activeRole,
			List<String> roles,
			boolean pendingTeacher) {
		public AuthenticatedUser {
			roles = roles == null ? List.of() : List.copyOf(roles);
		}
	}
}
