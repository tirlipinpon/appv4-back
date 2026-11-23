package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Forme de question notée sur une matière (SCRUM-161, première ligne en SCRUM-174).
 *
 * <p><b>Business :</b> « cette matière propose le choix multiple ». Une seule ligne
 * par code. Les énoncés sont ailleurs.
 * <p><b>Technique :</b> table {@code back_subject_question_forms}, déjà créée vide.
 * <p><b>Métier / flow :</b> écrite par {@code TeacherQuestionPersistenceAdapter}
 * à la première question de ce code.
 */
@Entity
@Table(name = "back_subject_question_forms")
public class BackSubjectQuestionFormEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "subject_id", nullable = false)
	private long subjectId;

	@Column(name = "form_code", nullable = false, length = 40)
	private String formCode;

	@Column(nullable = false, length = 100)
	private String label;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectQuestionFormEntity() {
	}

	public BackSubjectQuestionFormEntity(long subjectId, String formCode, String label, int sortOrder) {
		this.subjectId = subjectId;
		this.formCode = formCode;
		this.label = label;
		this.sortOrder = sortOrder;
	}

	public Long getId() {
		return id;
	}

	public long getSubjectId() {
		return subjectId;
	}

	public String getFormCode() {
		return formCode;
	}
}
