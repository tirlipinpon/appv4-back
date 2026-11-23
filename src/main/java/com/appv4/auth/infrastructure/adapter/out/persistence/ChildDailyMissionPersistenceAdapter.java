package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.port.out.ChildDailyMissionPort;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.NewMission;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.NewTask;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredMission;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredTask;

import jakarta.persistence.EntityManager;

/**
 * Persiste la mission du jour (BACK-18).
 *
 * <p><b>Business :</b> une seule mission par enfant et par date, même si deux
 * lectures arrivent en même temps.
 * <p><b>Technique :</b> insert Postgres {@code ON CONFLICT DO NOTHING}, puis les tâches.
 * Absent du profil {@code test}.
 * <p><b>Métier / flow :</b> adapter out du port {@link ChildDailyMissionPort}.
 */
@Component
@Profile("!test")
public class ChildDailyMissionPersistenceAdapter implements ChildDailyMissionPort {

	private final BackChildDailyMissionJpaRepository missions;
	private final BackChildDailyTaskJpaRepository tasks;
	private final EntityManager entityManager;

	public ChildDailyMissionPersistenceAdapter(
			BackChildDailyMissionJpaRepository missions,
			BackChildDailyTaskJpaRepository tasks,
			EntityManager entityManager) {
		this.missions = missions;
		this.tasks = tasks;
		this.entityManager = entityManager;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<StoredMission> findByChildAndDate(long childId, LocalDate missionDate) {
		return missions.findByChildIdAndMissionDate(childId, missionDate).map(this::toStored);
	}

	@Override
	@Transactional
	@SuppressWarnings("unchecked")
	public StoredMission insert(NewMission draft) {
		List<Number> ids = entityManager.createNativeQuery("""
				INSERT INTO back_child_daily_missions (child_id, mission_date, objective)
				VALUES (:childId, :missionDate, :objective)
				ON CONFLICT (child_id, mission_date) DO NOTHING
				RETURNING id
				""")
				.setParameter("childId", draft.childId())
				.setParameter("missionDate", draft.missionDate())
				.setParameter("objective", draft.objective())
				.getResultList();
		if (ids.isEmpty()) {
			return findByChildAndDate(draft.childId(), draft.missionDate())
					.orElseThrow(() -> new IllegalStateException("Mission du jour introuvable après conflit."));
		}
		long missionId = ids.get(0).longValue();
		for (NewTask task : draft.tasks()) {
			tasks.save(new BackChildDailyTaskEntity(
					missionId,
					task.position(),
					task.label(),
					task.targetCount(),
					task.rewardXp(),
					task.subjectId(),
					task.questionId()));
		}
		tasks.flush();
		return findByChildAndDate(draft.childId(), draft.missionDate())
				.orElseThrow(() -> new IllegalStateException("Mission du jour introuvable après insert."));
	}

	@Override
	@Transactional(readOnly = true)
	public List<StoredMission> missionsOf(long childId) {
		return missions.findByChildIdOrderByMissionDateAsc(childId).stream().map(this::toStored).toList();
	}

	@Override
	@Transactional
	public void rememberFinishedDay(long childId, LocalDate missionDate) {
		entityManager.createNativeQuery("""
				INSERT INTO back_child_mission_days (child_id, mission_date)
				VALUES (:childId, :missionDate)
				ON CONFLICT DO NOTHING
				""")
				.setParameter("childId", childId)
				.setParameter("missionDate", missionDate)
				.executeUpdate();
	}

	@Override
	@Transactional(readOnly = true)
	@SuppressWarnings("unchecked")
	public List<LocalDate> finishedDays(long childId) {
		List<Object> rows = entityManager.createNativeQuery("""
				SELECT mission_date FROM back_child_mission_days
				WHERE child_id = :childId
				ORDER BY mission_date
				""")
				.setParameter("childId", childId)
				.getResultList();
		List<LocalDate> days = new ArrayList<>();
		for (Object row : rows) {
			if (row instanceof LocalDate date) {
				days.add(date);
			} else if (row instanceof java.sql.Date date) {
				days.add(date.toLocalDate());
			}
		}
		return days;
	}

	@Override
	@Transactional
	public void setProgress(long taskId, int progressCount) {
		BackChildDailyTaskEntity task = tasks.findById(taskId)
				.orElseThrow(() -> new IllegalStateException("Tâche introuvable."));
		task.setProgressCount(progressCount);
		tasks.save(task);
	}

	private StoredMission toStored(BackChildDailyMissionEntity entity) {
		List<StoredTask> rows = new ArrayList<>();
		for (BackChildDailyTaskEntity task : tasks.findByMissionIdOrderByPositionAsc(entity.getId())) {
			rows.add(new StoredTask(
					task.getId(),
					task.getPosition(),
					task.getLabel(),
					task.getTargetCount(),
					task.getProgressCount(),
					task.getRewardXp(),
					task.getSubjectId(),
					task.getQuestionId()));
		}
		return new StoredMission(
				entity.getId(),
				entity.getChildId(),
				entity.getMissionDate(),
				entity.getObjective(),
				rows);
	}
}
