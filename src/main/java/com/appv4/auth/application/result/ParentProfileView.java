package com.appv4.auth.application.result;

import java.util.List;

/**
 * Fiche lue ou enregistrée dans l'espace parent.
 *
 * <p><b>Business :</b> le parent voit son mail de connexion, et corrige son nom, son mail de
 * contact, son téléphone et son avatar. Les écoles du professeur ne sont pas dans cette fiche.
 * <p><b>Technique :</b> {@code email} est le mail de connexion, en lecture seule.
 * <p><b>Métier / flow :</b> résultat du port in parent, renvoyé par {@code /api/int/parent/profile}.
 */
public record ParentProfileView(
		String email,
		String displayName,
		String contactEmail,
		String phone,
		String avatarCode,
		List<AvatarChoice> avatarChoices) {
}
