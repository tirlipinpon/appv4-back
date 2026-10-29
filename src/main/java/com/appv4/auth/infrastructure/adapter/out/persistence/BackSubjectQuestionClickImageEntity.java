package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Image d'une question à cliquer (SCRUM-181).
 *
 * <p><b>Business :</b> le professeur dépose un dessin. On garde la version légère,
 * pas le fichier d'origine.
 * <p><b>Technique :</b> table {@code back_subject_question_click_images}. Une ligne par question.
 * {@code imageWebp} est le fichier déjà réduit. {@code requireAllCorrect} dit si chaque
 * zone juste devra être cliquée.
 * <p><b>Métier / flow :</b> écrite avec la question dont la forme est {@code image_interactive}.
 * Les zones sont dans {@link BackSubjectQuestionClickZoneEntity}.
 */
@Entity
@Table(name = "back_subject_question_click_images")
public class BackSubjectQuestionClickImageEntity {

	@Id
	@Column(name = "question_id", nullable = false)
	private Long questionId;

	@Column(name = "image_webp", nullable = false)
	private byte[] imageWebp;

	@Column(name = "require_all_correct", nullable = false)
	private boolean requireAllCorrect;

	protected BackSubjectQuestionClickImageEntity() {
	}

	public BackSubjectQuestionClickImageEntity(long questionId, byte[] imageWebp, boolean requireAllCorrect) {
		this.questionId = questionId;
		this.imageWebp = imageWebp.clone();
		this.requireAllCorrect = requireAllCorrect;
	}

	public Long getQuestionId() {
		return questionId;
	}

	public byte[] getImageWebp() {
		return imageWebp.clone();
	}

	public boolean isRequireAllCorrect() {
		return requireAllCorrect;
	}
}
