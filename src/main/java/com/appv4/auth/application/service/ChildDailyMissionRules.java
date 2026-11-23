package com.appv4.auth.application.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredMission;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredTask;
import com.appv4.auth.application.result.ChildTodayView;
import com.appv4.auth.application.result.ChildTodayView.Mission;
import com.appv4.auth.application.result.ChildTodayView.Task;

/**
 * Règles figées de la mission du jour (BACK-18).
 *
 * <p><b>Business :</b> le front n'invente ni l'objectif, ni la progression, ni le
 * lien d'activité.
 * <p><b>Technique :</b> fonctions pures. Au plus 3 tâches, 1 question chacune, 10 XP.
 * <p><b>Métier / flow :</b> création du plan + lecture GET + tentative utile.
 */
public final class ChildDailyMissionRules {

	public static final int MAX_TASKS = 3;
	public static final int TARGET_PER_TASK = 1;
	public static final int REWARD_XP = 10;

	public static final String STATUS_TODO = "a_faire";
	public static final String STATUS_DOING = "en_cours";
	public static final String STATUS_DONE = "faite";
	public static final String STATUS_MISSION_DONE = "terminee";

	private ChildDailyMissionRules() {
	}

	public static String objective(int taskCount) {
		if (taskCount <= 1) {
			return "Joue 1 question aujourd'hui";
		}
		return "Joue " + taskCount + " questions aujourd'hui";
	}

	/** Une tentative compte si le back l'a acceptée avec un score ou une réussite. */
	public static boolean useful(int scorePercent, boolean passed) {
		return passed || scorePercent > 0;
	}

	public static int nextProgress(int progress, int target) {
		if (progress >= target) {
			return progress;
		}
		return progress + 1;
	}

	public static long secondsRemaining(Instant now, ZoneId zone) {
		ZonedDateTime zoned = now.atZone(zone);
		ZonedDateTime end = zoned.toLocalDate().plusDays(1).atStartOfDay(zone);
		return Math.max(Duration.between(zoned, end).getSeconds(), 0);
	}

	public static ChildTodayView toView(StoredMission stored, long secondsRemaining) {
		List<Task> tasks = new ArrayList<>();
		int progress = 0;
		int target = 0;
		int rewardXp = 0;
		for (StoredTask row : stored.tasks()) {
			Task task = toTask(row);
			tasks.add(task);
			progress += task.progress();
			target += task.target();
			rewardXp += task.rewardXp();
		}
		Task next = tasks.stream().filter(Task::playable).findFirst().orElse(null);
		boolean playable = next != null;
		boolean finished = !tasks.isEmpty() && !playable;
		return new ChildTodayView(new Mission(
				stored.id(),
				stored.objective(),
				progress,
				target,
				rewardXp,
				finished ? STATUS_MISSION_DONE : STATUS_DOING,
				finished ? "Terminée" : "En cours",
				secondsRemaining,
				playable ? next.subjectId() : null,
				playable ? next.questionId() : null,
				playable,
				tasks));
	}

	private static Task toTask(StoredTask row) {
		boolean done = row.progressCount() >= row.targetCount();
		boolean started = row.progressCount() > 0 && !done;
		String status = done ? STATUS_DONE : started ? STATUS_DOING : STATUS_TODO;
		String label = done ? "Faite" : started ? "En cours" : "À faire";
		return new Task(
				row.id(),
				row.label(),
				row.progressCount(),
				row.targetCount(),
				row.rewardXp(),
				status,
				label,
				row.subjectId(),
				row.questionId(),
				!done);
	}
}
