package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.ChildAvatarView;

/**
 * Enregistre le style et la palette de l'enfant (BACK-66).
 *
 * <p><b>Business :</b> le choix tient sans XP. Une palette d'un autre style est refusée.
 * <p><b>Technique :</b> {@code PUT /api/ext/avatar}, jeton enfant. Pas de variante dans cet appel.
 * <p><b>Métier / flow :</b> controller → ce port → catalogue figé → ligne d'avatar.
 */
public interface SaveChildAvatarStyleUseCase {

	ChildAvatarView saveStyle(String rawToken, String styleCode, String presetId);
}
