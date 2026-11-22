package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Invitation à tenir une fiche (BACK-74).
 *
 * <p><b>Business :</b> l'email est invité. Accepter donne la fiche. Annuler ou retirer la fiche ferme l'invitation.
 * <p><b>Technique :</b> table {@code back_child_invitations}. L'email est déjà en minuscules.
 * <p><b>Métier / flow :</b> le parent référent invite, le compte de cet email accepte.
 */
@Entity
@Table(name = "back_child_invitations")
public class BackChildInvitationEntity {

	public static final String PENDING = "pending";
	public static final String ACCEPTED = "accepted";
	public static final String CANCELLED = "cancelled";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "child_id", nullable = false)
	private long childId;

	@Column(nullable = false, length = 320)
	private String email;

	@Column(nullable = false, length = 16)
	private String status;

	protected BackChildInvitationEntity() {
	}

	public BackChildInvitationEntity(long childId, String email) {
		this.childId = childId;
		this.email = email;
		this.status = PENDING;
	}

	public long getId() {
		return id;
	}

	public long getChildId() {
		return childId;
	}

	public String getEmail() {
		return email;
	}

	public String getStatus() {
		return status;
	}

	public boolean isPending() {
		return PENDING.equals(status);
	}

	public void accept() {
		this.status = ACCEPTED;
	}

	public void cancel() {
		this.status = CANCELLED;
	}
}
