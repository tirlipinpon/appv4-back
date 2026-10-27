package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Énoncé préparé dans une matière (SCRUM-174).
 *
 * <p><b>Business :</b> le texte que l'élève lira plus tard, rattaché à la matière
 * et à une forme ({@code qcm}).
 * <p><b>Technique :</b> table {@code back_subject_questions}.
 * <p><b>Métier / flow :</b> écrite quand le professeur enregistre, lue quand il
 * rouvre la matière.
 */
@Entity
@Table(name = "back_subject_questions")
public class BackSubjectQuestionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "subject_id", nullable = false)
	private long subjectId;

	@Column(name = "form_code", nullable = false, length = 40)
	private String formCode;

	@Column(nullable = false, length = 500)
	private String prompt;

	protected BackSubjectQuestionEntity() {
	}

	public BackSubjectQuestionEntity(long subjectId, String formCode, String prompt) {
		this.subjectId = subjectId;
		this.formCode = formCode;
		this.prompt = prompt;
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

	public String getPrompt() {
		return prompt;
	}

	/** Le professeur corrige l'énoncé. La forme et la matière ne changent pas. */
	public void setPrompt(String prompt) {
		this.prompt = prompt;
	}
}
