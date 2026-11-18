package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Options d'une question Ordre (BACK-46).
 *
 * <p><b>Business :</b> le professeur peut activer une ligne du temps ; sinon l'élève
 * ne voit que la liste d'étapes.
 * <p><b>Technique :</b> table 1:1 {@code back_subject_question_order_options}.
 * <p><b>Métier / flow :</b> écrite avec les items Ordre par {@link ItemQuestionContentStore}.
 */
@Entity
@Table(name = "back_subject_question_order_options")
public class BackSubjectQuestionOrderOptionsEntity {

	@Id
	@Column(name = "question_id")
	private long questionId;

	@Column(name = "timeline_enabled", nullable = false)
	private boolean timelineEnabled;

	protected BackSubjectQuestionOrderOptionsEntity() {
	}

	public BackSubjectQuestionOrderOptionsEntity(long questionId, boolean timelineEnabled) {
		this.questionId = questionId;
		this.timelineEnabled = timelineEnabled;
	}

	public long getQuestionId() {
		return questionId;
	}

	public boolean isTimelineEnabled() {
		return timelineEnabled;
	}
}
