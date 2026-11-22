package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Proposition pour qu'un parent déjà sur la fiche en devienne le parent référent (BACK-76).
 *
 * <p><b>Business :</b> un message, un destinataire, et une seule proposition en attente par fiche.
 * <p><b>Technique :</b> table {@code back_child_transfers}. Le message fait au plus 200 caractères.
 * <p><b>Métier / flow :</b> l'acceptation change le parent référent. Un refus ou une annulation le laisse.
 */
@Entity
@Table(name = "back_child_transfers")
public class BackChildTransferEntity {

	public static final String PENDING = "pending";
	public static final String ACCEPTED = "accepted";
	public static final String REFUSED = "refused";
	public static final String CANCELLED = "cancelled";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "child_id", nullable = false)
	private long childId;

	@Column(name = "from_user_id", nullable = false)
	private long fromUserId;

	@Column(name = "to_user_id", nullable = false)
	private long toUserId;

	@Column(nullable = false, length = 200)
	private String message;

	@Column(nullable = false, length = 16)
	private String status;

	protected BackChildTransferEntity() {
	}

	public BackChildTransferEntity(long childId, long fromUserId, long toUserId, String message) {
		this.childId = childId;
		this.fromUserId = fromUserId;
		this.toUserId = toUserId;
		this.message = message;
		this.status = PENDING;
	}

	public long getId() {
		return id;
	}

	public long getChildId() {
		return childId;
	}

	public long getFromUserId() {
		return fromUserId;
	}

	public long getToUserId() {
		return toUserId;
	}

	public String getMessage() {
		return message;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
