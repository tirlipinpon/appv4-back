package com.appv4.auth.application.port.in;

import java.util.List;

/**
 * Séjour de l'enfant : signaux, lecture enfant, lecture parent (BACK-38).
 *
 * <p><b>Business :</b> l'enfant envoie des signaux. Le parent lit le temps confirmé,
 * déjà libellé. L'écran n'ajoute pas de secondes.
 * <p><b>Technique :</b> un seul cas d'usage. L'horloge est celle du bean.
 * <p><b>Métier / flow :</b> {@code POST /api/ext/sejour/signals}, {@code GET /api/ext/sejour},
 * {@code GET /api/int/parent/children/{childId}/sejours}.
 */
public interface SejourUseCase {

	void signal(String rawToken, String kind, String deviceId, String screen);

	/**
	 * @param holds vrai si cet appareil tient le séjour ouvert
	 */
	ChildSejour readChild(String rawToken, String deviceId);

	List<ParentSejour> readParent(long parentUserId, String activeRole, long childId);

	record ChildSejour(boolean holds) {
	}

	/**
	 * @param lines une ligne par écran, dans l'ordre de la première visite
	 */
	record ParentSejour(String dateLabel, String timeLabel, String durationLabel, List<Line> lines) {
	}

	/**
	 * @param label libellé d'écran déjà choisi (Accueil, Matières, …)
	 */
	record Line(String label, String durationLabel) {
	}
}
