package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Thème d'une matière pour le board (BACK-33).
 */
@Entity
@Table(name = "back_subject_topics")
public class BackSubjectTopicEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "subject_id", nullable = false)
	private long subjectId;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "icon_code", length = 128)
	private String iconCode;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectTopicEntity() {
	}

	public Long getId() {
		return id;
	}

	public long getSubjectId() {
		return subjectId;
	}

	public String getName() {
		return name;
	}

	public String getIconCode() {
		return iconCode;
	}

	public int getSortOrder() {
		return sortOrder;
	}
}
