package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.ChildAvatarView;

/**
 * Lecture du personnage de l'enfant connecté (BACK-48).
 *
 * <p><b>Business :</b> Profil montre le personnage, les styles et les zones fermées.
 * <p><b>Technique :</b> {@code GET /api/ext/avatar}, jeton enfant.
 * <p><b>Métier / flow :</b> controller → ce port → XP déjà stocké et catalogue figé.
 */
public interface ReadChildAvatarUseCase {

	ChildAvatarView avatar(String rawToken);
}
