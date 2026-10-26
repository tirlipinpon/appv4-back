package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Accès d'un professeur à une matière (SCRUM-172).
 *
 * <p><b>Business :</b> plusieurs professeurs peuvent agir sur la même matière
 * (partage). Déplacer ne laisse qu'une ligne d'accès.
 * <p><b>Technique :</b> table {@code back_teacher_subject_access}.
 * <p><b>Métier / flow :</b> créé à l'ajout de matière, enrichi par
 * {@code POST .../transfer}, retiré par DELETE matière.
 */
@Entity
@Table(name = "back_teacher_subject_access")
public class BackTeacherSubjectAccessEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "subject_id", nullable = false)
	private long subjectId;

	@Column(name = "user_id", nullable = false)
	private long userId;

	protected BackTeacherSubjectAccessEntity() {
	}

	public BackTeacherSubjectAccessEntity(long subjectId, long userId) {
		this.subjectId = subjectId;
		this.userId = userId;
	}

	public Long getId() {
		return id;
	}

	public long getSubjectId() {
		return subjectId;
	}

	public long getUserId() {
		return userId;
	}
}
