package com.appv4.auth.application.port.in;

import com.appv4.auth.application.port.out.ChildRankingRosterPort.ChildPhoto;
import com.appv4.auth.application.result.ChildRankingView;

/**
 * Lecture du classement pour l'enfant connecté (BACK-22).
 *
 * <p><b>Business :</b> l'enfant voit sa place par XP, et seulement les enfants
 * dont le parent a laissé la case cochée.
 * <p><b>Technique :</b> le jeton est celui du profil enfant. Pas de jeton adulte.
 * <p><b>Métier / flow :</b> port in de {@code GET /api/ext/ranking}.
 */
public interface ReadChildRankingUseCase {

	ChildRankingView read(String rawToken, String scope);

	/**
	 * Photo d'un enfant qui est membre du groupe demandé.
	 *
	 * <p><b>Business :</b> le classement n'ouvre pas l'album de toute l'application.
	 */
	ChildPhoto photo(String rawToken, String scope, long childId);
}
