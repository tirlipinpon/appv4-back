package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Une année cochée pour une école du professeur (SCRUM-159).
 *
 * <p><b>Business :</b> la classe où il donne cours dans cette école (CP, 1re primaire).
 * <p><b>Technique :</b> table {@code back_teacher_school_grades}. Le libellé est la valeur fermée.
 */
@Entity
@Table(
		name = "back_teacher_school_grades",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_back_teacher_school_grades_link_grade",
				columnNames = { "teacher_school_id", "grade_label" }))
public class BackTeacherSchoolGradeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "teacher_school_id", nullable = false)
	private long teacherSchoolId;

	@Column(name = "grade_label", nullable = false, length = 80)
	private String gradeLabel;

	protected BackTeacherSchoolGradeEntity() {
	}

	public BackTeacherSchoolGradeEntity(long teacherSchoolId, String gradeLabel) {
		this.teacherSchoolId = teacherSchoolId;
		this.gradeLabel = gradeLabel;
	}

	public long getTeacherSchoolId() {
		return teacherSchoolId;
	}

	public String getGradeLabel() {
		return gradeLabel;
	}
}
