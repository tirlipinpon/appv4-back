package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un mot d'une phrase à trous (SCRUM-179).
 *
 * <p><b>Business :</b> le professeur écrit la phrase avec ___, le mot juste de
 * chaque trou, et les leurres de la banque.
 * L'élève ne place pas encore les mots.
 * <p><b>Technique :</b> table {@code back_subject_question_gaps}. La phrase est
 * répétée sur chaque ligne pour rester dans une seule lecture.
 * <p><b>Métier / flow :</b> plusieurs lignes pour une {@link BackSubjectQuestionEntity}
 * dont la forme est {@code case_vide}.
 */
@Entity
@Table(name = "back_subject_question_gaps")
public class BackSubjectQuestionGapEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Column(nullable = false, length = 500)
	private String sentence;

	@Column(name = "word_label", nullable = false, length = 200)
	private String wordLabel;

	@Column(nullable = false)
	private boolean decoy;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectQuestionGapEntity() {
	}

	public BackSubjectQuestionGapEntity(
			long questionId, String sentence, String wordLabel, boolean decoy, int sortOrder) {
		this.questionId = questionId;
		this.sentence = sentence;
		this.wordLabel = wordLabel;
		this.decoy = decoy;
		this.sortOrder = sortOrder;
	}

	public long getQuestionId() {
		return questionId;
	}

	public Long getId() {
		return id;
	}

	public String getSentence() {
		return sentence;
	}

	public String getWordLabel() {
		return wordLabel;
	}

	public boolean isDecoy() {
		return decoy;
	}
}
