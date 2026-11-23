package com.appv4.auth.application.port.in;

import com.appv4.auth.application.port.out.ChildLoginPort.ChildPhotoBytes;
import com.appv4.auth.application.result.ChildProfileView;

/**
 * Port entrant — lire le profil de l'enfant connecté (BACK-14).
 *
 * <p><b>Business :</b> l'accueil learning montre le prénom et l'avatar/photo de la fiche.
 * <p><b>Technique :</b> jeton enfant obligatoire ; pas d'écriture.
 * <p><b>Métier / flow :</b> controller {@code /api/ext/me} → ce port → fiche active.
 */
public interface ReadChildProfileUseCase {

	ChildProfileView profile(String rawToken);

	ChildPhotoBytes photo(String rawToken);
}
