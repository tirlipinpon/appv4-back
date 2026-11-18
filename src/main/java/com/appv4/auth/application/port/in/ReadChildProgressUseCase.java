package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.ChildProgressView;

/**
 * Port entrant — lire la progression de l'enfant connecté (BACK-17).
 *
 * <p><b>Business :</b> home / mission / classements futurs partagent les mêmes chiffres.
 * <p><b>Technique :</b> jeton enfant ; pas de formule côté front.
 * <p><b>Métier / flow :</b> {@code GET /api/ext/progress} → ce port → table progress.
 */
public interface ReadChildProgressUseCase {

	ChildProgressView progress(String rawToken);
}
