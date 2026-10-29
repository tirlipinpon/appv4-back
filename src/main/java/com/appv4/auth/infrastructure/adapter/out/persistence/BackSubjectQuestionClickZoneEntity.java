package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Zone dessinée sur une image à cliquer (SCRUM-181).
 *
 * <p><b>Business :</b> un nom, un vert ou un rouge, et la forme que le professeur a ajustée.
 * <p><b>Technique :</b> table {@code back_subject_question_click_zones}. {@code points}
 * garde des coordonnées entre 0 et 1, pour que la forme suive l'image si l'écran change.
 * <p><b>Métier / flow :</b> plusieurs lignes pour une question de forme {@code image_interactive}.
 */
@Entity
@Table(name = "back_subject_question_click_zones")
public class BackSubjectQuestionClickZoneEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Column(nullable = false, length = 200)
	private String label;

	@Column(nullable = false)
	private boolean correct;

	@Column(nullable = false, length = 16)
	private String shape;

	@Column(nullable = false, length = 800)
	private String points;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectQuestionClickZoneEntity() {
	}

	public BackSubjectQuestionClickZoneEntity(
			long questionId, String label, boolean correct, String shape, String points, int sortOrder) {
		this.questionId = questionId;
		this.label = label;
		this.correct = correct;
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

	public boolean isCorrect() {
		return correct;
	}

	public String getShape() {
		return shape;
	}

	public String getPoints() {
		return points;
	}
}
