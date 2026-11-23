package com.appv4.auth.application.port.in;

import java.util.List;

/**
 * Séjour de l'enfant : signaux, lecture enfant, lecture parent (BACK-38, question BACK-79).
 *
 * <p><b>Business :</b> l'enfant envoie des signaux. Le jeu compte sur la question,
 * avec l'énoncé et la matière copiés. Le parent lit le temps confirmé, déjà libellé.
 * De 22 h à 6 h, la route enfant est refusée avec une seule phrase. L'écran n'ajoute pas de secondes.
 * <p><b>Technique :</b> un seul cas d'usage. L'horloge est celle du bean, en Europe/Paris.
 * <p><b>Métier / flow :</b> {@code POST /api/ext/sejour/signals}, {@code GET /api/ext/sejour},
 * les autres routes {@code /api/ext/**}, et {@code GET /api/int/parent/children/{childId}/sejours}.
 */
public interface SejourUseCase {

	/**
	 * Ferme le séjour ouvert à 22 h, puis refuse, quand l'horloge est dans la Nuit.
	 * Hors Nuit, ne fait rien.
	 *
	 * @param rawToken jeton enfant, vide si l'appel n'en a pas
	 */
	void refuseDuringNuit(String rawToken);

	/**
	 * @param questionId identifiant de la question jouée, seulement pour un signal question
	 */
	void signal(String rawToken, String kind, String deviceId, String screen, Long questionId);

	/**
	 * Signal d'écran, de geste, de présence ou de départ, sans question.
	 */
	default void signal(String rawToken, String kind, String deviceId, String screen) {
		signal(rawToken, kind, deviceId, screen, null);
	}

	/**
	 * @param holds vrai si cet appareil tient le séjour ouvert
	 */
	ChildSejour readChild(String rawToken, String deviceId);

	List<ParentSejour> readParent(long parentUserId, String activeRole, long childId);

	record ChildSejour(boolean holds) {
	}

	/**
	 * @param lines une ligne par écran ou par question, dans l'ordre de la première visite
	 */
	record ParentSejour(String dateLabel, String timeLabel, String durationLabel, List<Line> lines) {
	}

	/**
	 * @param label libellé d'écran, ou énoncé de la question
	 * @param subjectName nom de matière de la question, absent pour un écran
	 */
	record Line(String label, String subjectName, String durationLabel) {
	}
}
