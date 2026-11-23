package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.JwtTokenResult;
import com.appv4.auth.application.result.SpaceAccess;

/**
 * Port entrant — ajouter plus tard l'espace qui manque (SCRUM-152).
 *
 * <p><b>Business :</b> le choix parent / professeur ne reste pas bloqué au
 * formulaire d'inscription. Même email, même compte.
 * <p><b>Technique :</b> parent = ligne {@code back_user_roles}. Professeur =
 * demande {@code pending}, jamais le rôle teacher ici.
 * <p><b>Métier / flow :</b> personne déjà connectée → en-tête → ces trois
 * actions → {@code SignupRolePort} puis, pour le parent, un nouveau JWT.
 */
public interface AddMissingSpaceUseCase {

	/** Rôles accordés et état de la demande, pour l'en-tête après un F5. */
	SpaceAccess view(long userId);

	/**
	 * Accorde parent s'il manque, puis renvoie un jeton à jour.
	 * Une deuxième fois ne crée pas de doublon.
	 */
	JwtTokenResult openParent(long userId, String email);

	/**
	 * Ouvre une demande pending s'il n'y en a pas et si teacher n'est pas déjà accordé.
	 * Ne réémet pas de jeton : le rôle teacher n'est pas écrit.
	 */
	SpaceAccess requestTeacher(long userId);
}
