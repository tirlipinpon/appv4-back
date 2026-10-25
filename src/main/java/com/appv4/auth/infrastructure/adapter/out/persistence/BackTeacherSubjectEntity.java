package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Une matière d'un professeur, pour une école et une année (SCRUM-161).
 *
 * <p><b>Business :</b> le professeur la nomme. Elle ne se partage pas encore.
 * {@code originSubjectId} reste vide : un clone futur le remplira.
 * <p><b>Technique :</b> table {@code back_teacher_subjects}. Pas un objet du domaine.
 * Les formes de questions sont dans {@code back_subject_question_forms}, sans mapping
 * tant qu'aucune route ne les écrit.
 */
@Entity
@Table(
		name = "back_teacher_subjects",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_back_teacher_subjects_owner_class_name",
				columnNames = { "user_id", "school_id", "grade_label", "name_key" }))
public class BackTeacherSubjectEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private long userId;

	@Column(name = "school_id", nullable = false)
	private long schoolId;

	@Column(name = "grade_label", nullable = false, length = 80)
	private String gradeLabel;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "name_key", nullable = false, length = 100)
	private String nameKey;

	/**
	 * Vide à la création. Un clone futur pointe ici vers la matière d'origine.
	 */
	@Column(name = "origin_subject_id")
	private Long originSubjectId;

	protected BackTeacherSubjectEntity() {
	}

	public BackTeacherSubjectEntity(long userId, long schoolId, String gradeLabel, String name, String nameKey) {
		this.userId = userId;
		this.schoolId = schoolId;
		this.gradeLabel = gradeLabel;
		this.name = name;
		this.nameKey = nameKey;
	}

	public Long getId() {
		return id;
	}

	public long getUserId() {
		return userId;
	}

	public long getSchoolId() {
		return schoolId;
	}

	public String getGradeLabel() {
		return gradeLabel;
	}

	public String getName() {
		return name;
	}

	public String getNameKey() {
		return nameKey;
	}

	public void rename(String nextName, String nextKey) {
		this.name = nextName;
		this.nameKey = nextKey;
	}
}
