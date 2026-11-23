package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Un parent qui a la fiche (BACK-74).
 *
 * <p><b>Business :</b> le parent référent la gère. Un autre parent l'a en partage.
 * <p><b>Technique :</b> clé {@code child_id} + {@code user_id}. Un seul {@code referent} par fiche.
 * <p><b>Métier / flow :</b> écrite à l'acceptation d'une invitation, lue avec la liste des fiches.
 */
@Entity
@Table(name = "back_child_holders")
@IdClass(BackChildHolderEntity.Key.class)
public class BackChildHolderEntity {

	@Id
	@Column(name = "child_id", nullable = false)
	private long childId;

	@Id
	@Column(name = "user_id", nullable = false)
	private long userId;

	@Column(nullable = false)
	private boolean referent;

	protected BackChildHolderEntity() {
	}

	public BackChildHolderEntity(long childId, long userId, boolean referent) {
		this.childId = childId;
		this.userId = userId;
		this.referent = referent;
	}

	public long getChildId() {
		return childId;
	}

	public long getUserId() {
		return userId;
	}

	public boolean isReferent() {
		return referent;
	}

	public void setReferent(boolean referent) {
		this.referent = referent;
	}

	public static final class Key implements java.io.Serializable {
		private long childId;
		private long userId;

		public Key() {
		}

		public Key(long childId, long userId) {
			this.childId = childId;
			this.userId = userId;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof Key key)) {
				return false;
			}
			return childId == key.childId && userId == key.userId;
		}

		@Override
		public int hashCode() {
			return Long.hashCode(childId) * 31 + Long.hashCode(userId);
		}
	}
}
