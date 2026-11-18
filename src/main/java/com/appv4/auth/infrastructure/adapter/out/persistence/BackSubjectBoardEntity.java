package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Textes du board matière (BACK-33).
 */
@Entity
@Table(name = "back_subject_board")
public class BackSubjectBoardEntity {

	@Id
	@Column(name = "subject_id", nullable = false)
	private long subjectId;

	@Column(length = 500)
	private String description;

	@Column(length = 160)
	private String tagline;

	@Column(name = "world_label", length = 80)
	private String worldLabel;

	@Column(name = "age_label", length = 40)
	private String ageLabel;

	@Column(name = "level_label", length = 40)
	private String levelLabel;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	protected BackSubjectBoardEntity() {
	}

	public long getSubjectId() {
		return subjectId;
	}

	public String getDescription() {
		return description;
	}

	public String getTagline() {
		return tagline;
	}

	public String getWorldLabel() {
		return worldLabel;
	}

	public String getAgeLabel() {
		return ageLabel;
	}

	public String getLevelLabel() {
		return levelLabel;
	}
}
