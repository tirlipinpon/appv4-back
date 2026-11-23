package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Carte d'un classement, rattachée à sa colonne juste.
 *
 * <p><b>Business :</b> le texte que l'élève range ; la colonne juste reste côté serveur en jeu.
 * <p><b>Technique :</b> table {@code back_subject_question_cards}.
 * <p><b>Métier / flow :</b> écrites après les colonnes d'une question {@code classement}.
 */
@Entity
@Table(name = "back_subject_question_cards")
public class BackSubjectQuestionCardEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Column(name = "label", length = 200)
	private String label;

	@Column(name = "image_webp")
	private byte[] imageWebp;

	@Column(name = "correct_column_id", nullable = false)
	private long correctColumnId;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectQuestionCardEntity() {
	}

	public BackSubjectQuestionCardEntity(long questionId, String label, long correctColumnId, int sortOrder) {
		this(questionId, label, correctColumnId, sortOrder, null);
	}

	public BackSubjectQuestionCardEntity(
			long questionId, String label, long correctColumnId, int sortOrder, byte[] imageWebp) {
		this.questionId = questionId;
		this.label = label;
		this.correctColumnId = correctColumnId;
		this.sortOrder = sortOrder;
		this.imageWebp = imageWebp;
	}

	public Long getId() {
		return id;
	}

	public long getQuestionId() {
		return questionId;
	}

	public String getLabel() {
		return label;
	}

	public byte[] getImageWebp() {
		return imageWebp;
	}

	public long getCorrectColumnId() {
		return correctColumnId;
	}

	public int getSortOrder() {
		return sortOrder;
	}
}
