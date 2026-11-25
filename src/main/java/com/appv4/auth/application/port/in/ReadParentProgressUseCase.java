package com.appv4.auth.application.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
	 * @param xpTotal XP déjà accumulé, le même que l'accueil
	 * @param currentStreak série visible, le même compte que l'accueil
	 * @param leagueLabel Bronze, Argent, Or ou Diamant
	 * @param recentSuccess réussite des 3×24 h, ou absente
	 * @param days un jour du premier essai jusqu'à aujourd'hui, vide s'il n'y a aucun essai
	 * @param dailyMissionSummary mission du jour de Paris, absente s'il n'y a pas de ligne
	 * @param dailyMissionDays jours des semaines, du lundi de la première mission au dimanche courant
	 */
	record ParentProgress(
			String bestTimeSlotLabel,
			Integer averageSessionMinutes,
			Trend trend,
			List<FormeRatee> repeatedErrorPatterns,
			int xpTotal,
			int currentStreak,
			String leagueLabel,
			RecentSuccess recentSuccess,
			List<ProgressDay> days,
			MissionDay dailyMissionSummary,
			List<MissionDay> dailyMissionDays) {

		public ParentProgress {
			repeatedErrorPatterns = repeatedErrorPatterns == null ? List.of() : List.copyOf(repeatedErrorPatterns);
			days = days == null ? List.of() : List.copyOf(days);
			dailyMissionDays = dailyMissionDays == null ? List.of() : List.copyOf(dailyMissionDays);
		}
	}

	/**
	 * Un jour de mission, lu tel qu'il est gardé.
	 *
	 * @param failedEssais essais ratés encore là, absent s'il n'y a pas de mission
	 */
	record MissionDay(
			LocalDate date,
			String objective,
			Integer progress,
			Integer target,
			String status,
			String statusLabel,
			List<FailedEssai> failedEssais) {

		public MissionDay {
			if (failedEssais != null) {
				failedEssais = List.copyOf(failedEssais);
			}
		}
	}

	/**
	 * Essai raté d'une question de la mission.
	 *
	 * @param subjectLabel matière figée sur la tâche
	 * @param prompt texte actuel de la question, absent s'il est vide en base
	 * @param at instant de l'essai
	 */
	record FailedEssai(String subjectLabel, String prompt, Instant at) {
	}

	/**
	 * @param successRate part des essais réussis, au dixième
	 * @param attemptCount nombre de ces essais
	 * @param windowLabel phrase {@code 3 jours}
	 */
	record RecentSuccess(BigDecimal successRate, int attemptCount, String windowLabel) {
	}

	/**
	 * @param date jour à l'heure de la progression
	 * @param xpGained XP payé ce jour-là
	 * @param xpTotal XP accumulé à la fin de ce jour
	 * @param passed essais réussis, absent s'il n'y a aucun essai
	 * @param failed essais ratés, absent s'il n'y a aucun essai
	 */
	record ProgressDay(LocalDate date, int xpGained, int xpTotal, Integer passed, Integer failed) {
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
