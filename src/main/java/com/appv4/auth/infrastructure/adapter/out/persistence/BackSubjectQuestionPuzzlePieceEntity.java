package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Pièce d'un puzzle (SCRUM-183).
 *
 * <p><b>Business :</b> un nom et la forme que le professeur a ajustée. Pas de vert ni de rouge.
 * <p><b>Technique :</b> table {@code back_subject_question_puzzle_pieces}. {@code points}
 * garde des coordonnées entre 0 et 1, pour que la pièce suive l'image si l'écran change.
 * <p><b>Métier / flow :</b> plusieurs lignes pour une question de forme {@code puzzle}.
 * Le codage des points est celui de {@link ClickZonePoints}, déjà utilisé pour le clic.
 */
@Entity
@Table(name = "back_subject_question_puzzle_pieces")
public class BackSubjectQuestionPuzzlePieceEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Column(nullable = false, length = 200)
	private String label;

	@Column(nullable = false, length = 16)
	private String shape;

	@Column(nullable = false, length = 800)
	private String points;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectQuestionPuzzlePieceEntity() {
	}

	public BackSubjectQuestionPuzzlePieceEntity(
			long questionId, String label, String shape, String points, int sortOrder) {
		this.questionId = questionId;
		this.label = label;
		this.shape = shape;
		this.points = points;
		this.sortOrder = sortOrder;
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

	public String getShape() {
		return shape;
	}

	public String getPoints() {
		return points;
	}
}
