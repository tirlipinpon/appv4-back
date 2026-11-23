package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Une école rattachée à un professeur (SCRUM-159).
 *
 * <p><b>Business :</b> le professeur donne cours dans cette école, selon une norme.
 * L'école n'est pas créée ici : {@code school_id} pointe vers le futur annuaire.
 * <p><b>Technique :</b> table {@code back_teacher_schools}. Pas un objet du domaine.
 */
@Entity
@Table(
		name = "back_teacher_schools",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_back_teacher_schools_user_school",
				columnNames = { "user_id", "school_id" }))
public class BackTeacherSchoolEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private long userId;

	@Column(name = "school_id", nullable = false)
	private long schoolId;

	@Column(nullable = false, length = 32)
	private String curriculum;

	protected BackTeacherSchoolEntity() {
	}

	public BackTeacherSchoolEntity(long userId, long schoolId, String curriculum) {
		this.userId = userId;
		this.schoolId = schoolId;
		this.curriculum = curriculum;
	}

	public Long getId() {
		return id;
	}

	public long getSchoolId() {
		return schoolId;
	}

	public String getCurriculum() {
		return curriculum;
	}
}
