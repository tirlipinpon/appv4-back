package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Progression XP / streak / semaine d'un enfant (BACK-17).
 *
 * <p><b>Business :</b> une ligne par fiche enfant.
 * <p><b>Technique :</b> table {@code back_child_progress} ; PK = {@code child_id}.
 * <p><b>Métier / flow :</b> adapter out du port progress.
 */
@Entity
@Table(name = "back_child_progress")
public class BackChildProgressEntity {

	@Id
	@Column(name = "child_id", nullable = false)
	private long childId;

	@Column(name = "xp_total", nullable = false)
	private int xpTotal;

	@Column(name = "current_streak", nullable = false)
	private int currentStreak;

	@Column(name = "last_play_date")
	private LocalDate lastPlayDate;

	@Column(name = "week_start_date")
	private LocalDate weekStartDate;

	@Column(name = "week_days", nullable = false, length = 7)
	private String weekDays = "0000000";

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected BackChildProgressEntity() {
	}

	public BackChildProgressEntity(
			long childId,
			int xpTotal,
			int currentStreak,
			LocalDate lastPlayDate,
			LocalDate weekStartDate,
			String weekDays) {
		this.childId = childId;
		this.xpTotal = xpTotal;
		this.currentStreak = currentStreak;
		this.lastPlayDate = lastPlayDate;
		this.weekStartDate = weekStartDate;
		this.weekDays = weekDays;
		this.updatedAt = Instant.now();
	}

	public long getChildId() {
		return childId;
	}

	public int getXpTotal() {
		return xpTotal;
	}

	public int getCurrentStreak() {
		return currentStreak;
	}

	public LocalDate getLastPlayDate() {
		return lastPlayDate;
	}

	public LocalDate getWeekStartDate() {
		return weekStartDate;
	}

	public String getWeekDays() {
		return weekDays;
	}

	public void apply(
			int xpTotal,
			int currentStreak,
			LocalDate lastPlayDate,
			LocalDate weekStartDate,
			String weekDays) {
		this.xpTotal = xpTotal;
		this.currentStreak = currentStreak;
		this.lastPlayDate = lastPlayDate;
		this.weekStartDate = weekStartDate;
		this.weekDays = weekDays;
		this.updatedAt = Instant.now();
	}
}
