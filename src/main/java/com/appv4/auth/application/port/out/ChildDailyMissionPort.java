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
