package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Leçon d'un thème (BACK-33).
 */
@Entity
@Table(name = "back_subject_lessons")
public class BackSubjectLessonEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "topic_id", nullable = false)
	private long topicId;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "estimated_minutes", nullable = false)
	private int estimatedMinutes;

	@Column(name = "reward_xp", nullable = false)
	private int rewardXp;

	protected BackSubjectLessonEntity() {
	}

	public Long getId() {
		return id;
	}

	public long getTopicId() {
		return topicId;
	}

	public String getTitle() {
		return title;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public int getEstimatedMinutes() {
		return estimatedMinutes;
	}

	public int getRewardXp() {
		return rewardXp;
	}
}
