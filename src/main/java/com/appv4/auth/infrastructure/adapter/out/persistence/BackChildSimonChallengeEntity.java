package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Suite Simon tirée pour un enfant (BACK-55).
 *
 * <p><b>Business :</b> le professeur ne choisit pas l'ordre. Cette ligne est la suite que
 * l'enfant vient de voir, pour la noter sans qu'il puisse en envoyer une autre.
 * <p><b>Technique :</b> {@code back_child_simon_challenges}, clé (enfant, question),
 * {@code sequence} en {@code integer[]}.
 * <p><b>Métier / flow :</b> écrite à l'ouverture du jeu, lue à la tentative, puis retirée.
 */
@Entity
@Table(name = "back_child_simon_challenges")
@IdClass(ChildSimonChallengeKey.class)
public class BackChildSimonChallengeEntity {

	@Id
	@Column(name = "child_id")
	private long childId;

	@Id
	@Column(name = "question_id")
	private long questionId;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "sequence", nullable = false, columnDefinition = "integer[]")
	private Integer[] sequence;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected BackChildSimonChallengeEntity() {
	}

	public BackChildSimonChallengeEntity(long childId, long questionId, Integer[] sequence) {
		this.childId = childId;
		this.questionId = questionId;
		this.sequence = sequence;
		this.createdAt = Instant.now();
	}

	public Integer[] getSequence() {
		return sequence;
	}

	public void replace(Integer[] sequence) {
		this.sequence = sequence;
		this.createdAt = Instant.now();
	}
}
