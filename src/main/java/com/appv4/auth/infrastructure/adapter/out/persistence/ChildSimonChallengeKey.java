package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.io.Serializable;
import java.util.Objects;

/** Clé (enfant, question) d'une suite Simon tirée. */
public class ChildSimonChallengeKey implements Serializable {

	private long childId;
	private long questionId;

	public ChildSimonChallengeKey() {
	}

	public ChildSimonChallengeKey(long childId, long questionId) {
		this.childId = childId;
		this.questionId = questionId;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ChildSimonChallengeKey key)) {
			return false;
		}
		return childId == key.childId && questionId == key.questionId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(childId, questionId);
	}
}
