package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Images et plateau d'un puzzle (SCRUM-183).
 *
 * <p><b>Business :</b> une image pour découper les pièces, un mode de plateau
 * (fond / blanc / trous), et éventuellement une image de fond dédiée.
 * <p><b>Technique :</b> table {@code back_subject_question_puzzle_images}.
 * {@code boardImageWebp} est null hors mode {@code fond}.
 * <p><b>Métier / flow :</b> écrite avec la question dont la forme est {@code puzzle}.
 */
@Entity
@Table(name = "back_subject_question_puzzle_images")
public class BackSubjectQuestionPuzzleImageEntity {

	@Id
	@Column(name = "question_id", nullable = false)
	private Long questionId;

	@Column(name = "image_webp", nullable = false)
	private byte[] imageWebp;

	@Column(name = "board_mode", nullable = false, length = 16)
	private String boardMode;

	@Column(name = "board_image_webp")
	private byte[] boardImageWebp;

	@Column(name = "snap_distance", nullable = false)
	private double snapDistance;

	protected BackSubjectQuestionPuzzleImageEntity() {
	}

	public BackSubjectQuestionPuzzleImageEntity(
			long questionId,
			byte[] imageWebp,
			String boardMode,
			byte[] boardImageWebp,
			double snapDistance) {
		this.questionId = questionId;
		this.imageWebp = imageWebp.clone();
		this.boardMode = boardMode;
		this.boardImageWebp = boardImageWebp == null ? null : boardImageWebp.clone();
		this.snapDistance = snapDistance;
	}

	public Long getQuestionId() {
		return questionId;
	}

	public byte[] getImageWebp() {
		return imageWebp.clone();
	}

	public String getBoardMode() {
		return boardMode;
	}

	public byte[] getBoardImageWebp() {
		return boardImageWebp == null ? null : boardImageWebp.clone();
	}

	public double getSnapDistance() {
		return snapDistance;
	}
}
