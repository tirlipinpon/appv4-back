package com.appv4.auth.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Mission du jour et ses tâches (BACK-18).
 *
 * <p><b>Business :</b> un enfant a au plus une mission par jour, avec des tâches
 * liées à ses matières.
 * <p><b>Technique :</b> tables {@code back_child_daily_missions} et
 * {@code back_child_daily_tasks}.
 * <p><b>Métier / flow :</b> GET {@code /api/ext/today} crée ou relit ; une tentative
 * utile met à jour {@code progress_count}.
 */
public interface ChildDailyMissionPort {

	Optional<StoredMission> findByChildAndDate(long childId, LocalDate missionDate);

	/**
	 * Insère la mission du jour. Si une autre requête l'a déjà créée, renvoie celle-là
	 * sans ajouter de tâches en double.
	 */
	StoredMission insert(NewMission mission);

	void setProgress(long taskId, int progressCount);

	/**
	 * Fige le bonus sans faute de la mission, une seule fois.
	 *
	 * <p><b>Business :</b> 0 veut dire mission finie avec une erreur. Un montant
	 * positif est le bonus déjà ajouté aux XP. Vide : mission ancienne ou pas encore finie.
	 * <p><b>Technique :</b> {@code bonus_xp}, écrit seulement s'il est encore vide.
	 * <p><b>Métier / flow :</b> au moment où la dernière tâche de la mission se termine.
	 */
	default void setBonus(long missionId, int bonusXp) {
	}

	/** Bonus déjà figé, ou vide s'il ne l'est pas. */
	default Integer bonusOf(long missionId) {
		return null;
	}

	/**
	 * Jours dont la mission a été finie sans faute.
	 *
	 * <p><b>Business :</b> seuls les jours figés après cette règle comptent.
	 * Une mission plus ancienne, sans bonus, n'allonge pas la chaîne.
	 * <p><b>Technique :</b> {@code bonus_xp > 0}.
	 * <p><b>Métier / flow :</b> calcul du bonus du jour, avant de l'annoncer.
	 */
	default List<LocalDate> sansFauteDates(long childId) {
		return List.of();
	}

	/**
	 * Missions encore en base pour cet enfant.
	 *
	 * <p><b>Business :</b> une mission dont toutes les tâches sont encore là et finies compte une fois.
	 * <p><b>Technique :</b> défaut vide pour les tests qui ne s'en servent pas.
	 * <p><b>Métier / flow :</b> reprise des badges, avant le premier essai nouveau.
	 */
	default List<StoredMission> missionsOf(long childId) {
		return List.of();
	}

	/**
	 * Garde le jour où la mission a été finie, sans ses tâches.
	 *
	 * <p><b>Business :</b> supprimer une question ensuite n'efface pas ce jour.
	 * <p><b>Technique :</b> {@code back_child_mission_days}.
	 * <p><b>Métier / flow :</b> après une tentative qui termine la mission, et pendant la reprise.
	 */
	default void rememberFinishedDay(long childId, LocalDate missionDate) {
	}

	default List<LocalDate> finishedDays(long childId) {
		return List.of();
	}

	record NewMission(long childId, LocalDate missionDate, String objective, List<NewTask> tasks) {
		public NewMission {
			tasks = tasks == null ? List.of() : List.copyOf(tasks);
		}
	}

	record NewTask(
			int position,
			String label,
			int targetCount,
			int rewardXp,
			long subjectId,
			long questionId) {
	}

	record StoredMission(
			long id,
			long childId,
			LocalDate missionDate,
			String objective,
			List<StoredTask> tasks) {
		public StoredMission {
			tasks = tasks == null ? List.of() : List.copyOf(tasks);
		}
	}

	record StoredTask(
			long id,
			int position,
			String label,
			int targetCount,
			int progressCount,
			int rewardXp,
			long subjectId,
			long questionId) {
	}
}
