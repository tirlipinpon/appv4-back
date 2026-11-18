package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Lien leçon → question (BACK-33).
 */
@Entity
@Table(name = "back_subject_lesson_questions")
@IdClass(BackSubjectLessonQuestionEntity.Pk.class)
public class BackSubjectLessonQuestionEntity {

	@Id
	@Column(name = "lesson_id", nullable = false)
	private long lessonId;

	@Id
	@Column(name = "question_id", nullable = false)
	private long questionId;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	protected BackSubjectLessonQuestionEntity() {
	}

	public long getLessonId() {
		return lessonId;
	}

	public long getQuestionId() {
		return questionId;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public static final class Pk implements Serializable {
		private long lessonId;
		private long questionId;

		public Pk() {
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof Pk pk)) {
				return false;
			}
			return lessonId == pk.lessonId && questionId == pk.questionId;
		}

		@Override
		public int hashCode() {
			return Objects.hash(lessonId, questionId);
		}
	}
}
