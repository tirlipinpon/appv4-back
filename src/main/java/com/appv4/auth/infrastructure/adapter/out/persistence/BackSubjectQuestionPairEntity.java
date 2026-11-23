package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Une paire d'un Memory (SCRUM-175).
 *
 * <p><b>Business :</b> deux faces qui vont ensemble, saisies par le professeur.
 * L'élève ne retourne pas encore les cartes.
 * <p><b>Technique :</b> table {@code back_subject_question_pairs}. {@code sortOrder}
 * suit l'ordre saisi.
 * <p><b>Métier / flow :</b> plusieurs lignes pour une {@link BackSubjectQuestionEntity}
 * dont la forme est {@code memory}.
 */
@Entity
@Table(name = "back_subject_question_pairs")
public class BackSubjectQuestionPairEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Column(name = "question_side", length = 200)
	private String questionSide;

	@Column(name = "answer_side", length = 200)
	private String answerSide;

	@Column(name = "question_image_webp")
	private byte[] questionImageWebp;

	@Column(name = "answer_image_webp")
	private byte[] answerImageWebp;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectQuestionPairEntity() {
	}

	public BackSubjectQuestionPairEntity(
			long questionId, String questionSide, String answerSide, int sortOrder) {
		this(questionId, questionSide, answerSide, sortOrder, null, null);
	}

	public BackSubjectQuestionPairEntity(
			long questionId, String questionSide, String answerSide, int sortOrder,
			byte[] questionImageWebp, byte[] answerImageWebp) {
		this.questionId = questionId;
		this.questionSide = questionSide;
		this.answerSide = answerSide;
		this.sortOrder = sortOrder;
		this.questionImageWebp = questionImageWebp;
		this.answerImageWebp = answerImageWebp;
	}

	public long getQuestionId() {
		return questionId;
	}

	public Long getId() {
		return id;
	}

	public String getQuestionSide() {
		return questionSide;
	}

	public byte[] getQuestionImageWebp() {
		return questionImageWebp;
	}

	public byte[] getAnswerImageWebp() {
		return answerImageWebp;
	}

	public String getAnswerSide() {
		return answerSide;
	}
}
