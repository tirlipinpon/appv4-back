package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Images d'un puzzle (SCRUM-183).
 *
 * <p><b>Business :</b> une image pour découper les pièces, une autre comme fond de jeu
 * où l'enfant les repose. On garde les versions légères WebP, pas les fichiers d'origine.
 * <p><b>Technique :</b> table {@code back_subject_question_puzzle_images}. Une ligne par question.
 * {@code imageWebp} = découpe. {@code boardImageWebp} = fond. {@code snapDistance} = magnétisme
 * (fraction de la largeur).
 * <p><b>Métier / flow :</b> écrite avec la question dont la forme est {@code puzzle}.
 * Les pièces sont dans {@link BackSubjectQuestionPuzzlePieceEntity}.
 */
@Entity
@Table(name = "back_subject_question_puzzle_images")
public class BackSubjectQuestionPuzzleImageEntity {

	@Id
	@Column(name = "question_id", nullable = false)
	private Long questionId;

	@Column(name = "image_webp", nullable = false)
	private byte[] imageWebp;

	@Column(name = "board_image_webp", nullable = false)
	private byte[] boardImageWebp;

	@Column(name = "snap_distance", nullable = false)
	private double snapDistance;

	protected BackSubjectQuestionPuzzleImageEntity() {
	}

	public BackSubjectQuestionPuzzleImageEntity(
			long questionId, byte[] imageWebp, byte[] boardImageWebp, double snapDistance) {
		this.questionId = questionId;
		this.imageWebp = imageWebp.clone();
		this.boardImageWebp = boardImageWebp.clone();
		this.snapDistance = snapDistance;
	}

	public Long getQuestionId() {
		return questionId;
	}

	public byte[] getImageWebp() {
		return imageWebp.clone();
	}

	public byte[] getBoardImageWebp() {
		return boardImageWebp.clone();
	}

	public double getSnapDistance() {
		return snapDistance;
	}
}
