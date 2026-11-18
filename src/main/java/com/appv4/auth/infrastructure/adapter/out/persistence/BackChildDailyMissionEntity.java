package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Mission du jour d'un enfant (BACK-18).
 *
 * <p><b>Business :</b> une ligne par fiche et par jour.
 * <p><b>Technique :</b> table {@code back_child_daily_missions}.
 * <p><b>Métier / flow :</b> adapter out du port mission.
 */
@Entity
@Table(name = "back_child_daily_missions")
public class BackChildDailyMissionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "child_id", nullable = false)
	private long childId;

	@Column(name = "mission_date", nullable = false)
	private LocalDate missionDate;

	@Column(nullable = false, length = 120)
	private String objective;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected BackChildDailyMissionEntity() {
	}

	public BackChildDailyMissionEntity(long childId, LocalDate missionDate, String objective) {
		this.childId = childId;
		this.missionDate = missionDate;
		this.objective = objective;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public long getChildId() {
		return childId;
	}

	public LocalDate getMissionDate() {
		return missionDate;
	}

	public String getObjective() {
		return objective;
	}
}
