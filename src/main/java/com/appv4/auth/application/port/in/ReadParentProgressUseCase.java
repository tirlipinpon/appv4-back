package com.appv4.auth.application.port.in;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lecture parent des quatre blocs du suivi.
 *
 * <p><b>Business :</b> le parent voit le créneau, la durée d'un séjour, la tendance
 * et les formes ratées. Il n'y a pas encore de page.
 * <p><b>Technique :</b> {@code GET /api/int/parent/children/{childId}/progress}.
 * Un bloc absent est nul. La liste des formes est vide, jamais nulle.
 * <p><b>Métier / flow :</b> port in après le contrôle du parent qui a la fiche.
 */
public interface ReadParentProgressUseCase {

	/**
	 * Quatre blocs du suivi.
	 *
	 * <p><b>Business :</b> le parent voit où l'enfant réussit, combien dure un séjour,
	 * si les 7 jours montent, et quelles formes il rate.
	 * <p><b>Technique :</b> rôle parent, fiche visible. Sinon le refus déjà connu.
	 * <p><b>Métier / flow :</b> controller parent → ce port. Rien n'est écrit.
	 */
	ParentProgress read(long parentUserId, String activeRole, long childId);

	/**
	 * @param bestTimeSlotLabel Matin, Après-midi, Soir, ou absent
	 * @param averageSessionMinutes moyenne arrondie, 0 compris, ou absente
	 * @param trend tendance publiée, ou absente
	 * @param repeatedErrorPatterns formes ratées, vide s'il n'y en a pas
	 */
	record ParentProgress(
			String bestTimeSlotLabel,
			Integer averageSessionMinutes,
			Trend trend,
			List<FormeRatee> repeatedErrorPatterns) {

		public ParentProgress {
			repeatedErrorPatterns = repeatedErrorPatterns == null ? List.of() : List.copyOf(repeatedErrorPatterns);
		}
	}

	/**
	 * @param direction hausse, baisse ou stable
	 * @param deltaSuccessPoints écart coupé au dixième
	 * @param windowLabel phrase {@code 7 jours}
	 */
	record Trend(String direction, BigDecimal deltaSuccessPoints, String windowLabel) {
	}

	/**
	 * @param formCode code catalogue
	 * @param count essais ratés
	 * @param label libellé catalogue
	 */
	record FormeRatee(String formCode, int count, String label) {
	}
}
