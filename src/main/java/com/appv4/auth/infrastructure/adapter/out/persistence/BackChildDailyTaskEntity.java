package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tâche d'une mission du jour (BACK-18).
 *
 * <p><b>Business :</b> une activité à jouer, avec une cible et des XP.
 * <p><b>Technique :</b> table {@code back_child_daily_tasks}.
 * <p><b>Métier / flow :</b> créée avec la mission, progress mise à jour après tentative.
 */
@Entity
@Table(name = "back_child_daily_tasks")
public class BackChildDailyTaskEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "mission_id", nullable = false)
	private long missionId;

	@Column(nullable = false)
	private int position;

	@Column(nullable = false, length = 100)
	private String label;

	@Column(name = "target_count", nullable = false)
	private int targetCount;

	@Column(name = "progress_count", nullable = false)
	private int progressCount;

	@Column(name = "reward_xp", nullable = false)
	private int rewardXp;

	@Column(name = "subject_id", nullable = false)
	private long subjectId;

	@Column(name = "question_id", nullable = false)
	private long questionId;

	protected BackChildDailyTaskEntity() {
	}

	public BackChildDailyTaskEntity(
			long missionId,
			int position,
			String label,
			int targetCount,
			int rewardXp,
			long subjectId,
			long questionId) {
		this.missionId = missionId;
		this.position = position;
		this.label = label;
		this.targetCount = targetCount;
		this.progressCount = 0;
		this.rewardXp = rewardXp;
		this.subjectId = subjectId;
		this.questionId = questionId;
	}

	public Long getId() {
		return id;
	}

	public int getPosition() {
		return position;
	}

	public String getLabel() {
		return label;
	}

	public int getTargetCount() {
		return targetCount;
	}

	public int getProgressCount() {
		return progressCount;
	}

	public int getRewardXp() {
		return rewardXp;
	}

	public long getSubjectId() {
		return subjectId;
	}

	public long getQuestionId() {
		return questionId;
	}

	public void setProgressCount(int progressCount) {
		this.progressCount = progressCount;
	}
}
