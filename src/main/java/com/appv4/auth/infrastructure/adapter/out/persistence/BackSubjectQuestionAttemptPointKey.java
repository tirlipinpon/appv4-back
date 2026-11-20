package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.io.Serializable;
import java.util.Objects;

/** Clé question + rang d'essai des points personnalisés. */
public class BackSubjectQuestionAttemptPointKey implements Serializable {

	private long questionId;
	private int sortOrder;

	public BackSubjectQuestionAttemptPointKey() {
	}

	public BackSubjectQuestionAttemptPointKey(long questionId, int sortOrder) {
		this.questionId = questionId;
		this.sortOrder = sortOrder;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BackSubjectQuestionAttemptPointKey key)) {
			return false;
		}
		return questionId == key.questionId && sortOrder == key.sortOrder;
	}

	@Override
	public int hashCode() {
		return Objects.hash(questionId, sortOrder);
	}
}
