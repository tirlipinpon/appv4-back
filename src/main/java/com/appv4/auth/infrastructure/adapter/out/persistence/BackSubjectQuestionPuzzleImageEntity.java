package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Image d'un puzzle (SCRUM-183).
 *
 * <p><b>Business :</b> le professeur dépose un dessin. On garde la version légère,
 * pas le fichier d'origine. Les pièces seront découpées dans cette image.
 * <p><b>Technique :</b> table {@code back_subject_question_puzzle_images}. Une ligne par question.
 * {@code imageWebp} est le fichier déjà réduit. {@code snapDistance} est le magnétisme,
 * en fraction de la largeur de l'image.
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

	@Column(name = "snap_distance", nullable = false)
	private double snapDistance;

	protected BackSubjectQuestionPuzzleImageEntity() {
	}

	public BackSubjectQuestionPuzzleImageEntity(long questionId, byte[] imageWebp, double snapDistance) {
		this.questionId = questionId;
		this.imageWebp = imageWebp.clone();
		this.snapDistance = snapDistance;
	}

	public Long getQuestionId() {
		return questionId;
	}

	public byte[] getImageWebp() {
		return imageWebp.clone();
	}

	public double getSnapDistance() {
		return snapDistance;
	}
}
