package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.ParentProfileView;

/**
 * Lire et corriger la fiche du compte depuis l'espace parent.
 *
 * <p><b>Business :</b> un parent met à jour son nom, son mail de contact, son téléphone et son
 * avatar. Le mail de connexion, le mot de passe et les rôles ne bougent pas.
 * <p><b>Technique :</b> le rôle actif doit être {@code parent}.
 * <p><b>Métier / flow :</b> port in appelé par le controller parent, puis le port out du compte.
 */
public interface ParentProfileUseCase {

	ParentProfileView load(long userId, String activeRole);

	ParentProfileView update(
			long userId,
			String activeRole,
			String displayName,
			String contactEmail,
			String phone,
			String avatarCode);
}
