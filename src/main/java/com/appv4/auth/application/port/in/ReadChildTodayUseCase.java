package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.ChildTodayView;

/**
 * Lecture de la mission du jour (BACK-18).
 *
 * <p><b>Business :</b> l'enfant retrouve le même plan tant que le jour n'a pas changé.
 * <p><b>Technique :</b> jeton enfant → mission du jour (créée si absente).
 * <p><b>Métier / flow :</b> {@code GET /api/ext/today} → ce port.
 */
public interface ReadChildTodayUseCase {

	ChildTodayView today(String rawToken);
}
