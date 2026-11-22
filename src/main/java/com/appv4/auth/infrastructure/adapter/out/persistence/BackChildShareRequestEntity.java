package com.appv4.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Demande de partage (BACK-75).
 *
 * <p><b>Business :</b> un parent demande une fiche par le nom, sans la voir avant l'acceptation.
 * <p><b>Technique :</b> table {@code back_child_share_requests}. Pas d'identifiant d'enfant.
 * <p><b>Métier / flow :</b> le parent référent accepte une des fiches qui portent ce nom.
 */
@Entity
@Table(name = "back_child_share_requests")
public class BackChildShareRequestEntity {

	public static final String PENDING = "pending";
	public static final String ACCEPTED = "accepted";
	public static final String REFUSED = "refused";
	public static final String CANCELLED = "cancelled";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "sender_user_id", nullable = false)
	private long senderUserId;

	@Column(name = "referent_user_id", nullable = false)
	private long referentUserId;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Column(nullable = false, length = 16)
	private String status;

	protected BackChildShareRequestEntity() {
	}

	public BackChildShareRequestEntity(long senderUserId, long referentUserId, String firstName, String lastName) {
		this.senderUserId = senderUserId;
		this.referentUserId = referentUserId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.status = PENDING;
	}

	public long getId() {
		return id;
	}

	public long getSenderUserId() {
		return senderUserId;
	}

	public long getReferentUserId() {
		return referentUserId;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
