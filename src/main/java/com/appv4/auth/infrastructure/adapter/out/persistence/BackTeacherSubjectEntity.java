package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Une matière d'un professeur, pour une école et une année (SCRUM-161).
 *
 * <p><b>Business :</b> le professeur la nomme. La désactiver la laisse dans sa liste
 * et la retire du reste de l'application. La retirer la cache, la ligne reste.
 * {@code originSubjectId} reste vide : un clone futur le remplira.
 * <p><b>Technique :</b> table {@code back_teacher_subjects}. Pas un objet du domaine.
 * L'unicité du nom ne concerne que les lignes encore affichées.
 */
@Entity
@Table(name = "back_teacher_subjects")
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

	@Column(nullable = false)
	private boolean active = true;

	@Column(nullable = false)
	private boolean hidden;

	protected BackTeacherSubjectEntity() {
	}

	public BackTeacherSubjectEntity(long userId, long schoolId, String gradeLabel, String name, String nameKey) {
		this.userId = userId;
		this.schoolId = schoolId;
		this.gradeLabel = gradeLabel;
		this.name = name;
		this.nameKey = nameKey;
		this.active = true;
		this.hidden = false;
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

	public boolean isActive() {
		return active;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void rename(String nextName, String nextKey) {
		this.name = nextName;
		this.nameKey = nextKey;
	}

	/** Reste dans la liste du professeur, plus proposée ailleurs. */
	public void deactivate() {
		this.active = false;
	}

	/** De nouveau proposée dans le reste de l'application. */
	public void reactivate() {
		this.active = true;
	}

	/** Disparaît de la page. La ligne reste. */
	public void hide() {
		this.hidden = true;
	}
}
