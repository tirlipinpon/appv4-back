package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.ChildBadgesView;

/**
 * Lecture des badges de l'enfant connecté (BACK-19).
 *
 * <p><b>Business :</b> la collection et l'accueil affichent cette lecture, ils ne la recalculent pas.
 * <p><b>Technique :</b> {@code GET /api/ext/badges}, jeton enfant.
 * <p><b>Métier / flow :</b> adapter in → ce port → étoiles et niveaux déjà gardés.
 */
public interface ReadChildBadgesUseCase {

	ChildBadgesView badges(String rawToken);
}
