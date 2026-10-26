package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Demande de transfert de matière en attente (SCRUM-189).
 *
 * <p><b>Business :</b> le collègue doit accepter avant que l'accès change.
 * <p><b>Technique :</b> {@code back_teacher_subject_transfer_requests}.
 * <p><b>Métier / flow :</b> créée par POST transfer → acceptée ou refusée
 * par le destinataire.
 */
@Entity
@Table(name = "back_teacher_subject_transfer_requests")
public class BackTeacherSubjectTransferRequestEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "subject_id", nullable = false)
	private long subjectId;

	@Column(name = "from_user_id", nullable = false)
	private long fromUserId;

	@Column(name = "to_user_id", nullable = false)
	private long toUserId;

	@Column(nullable = false, length = 10)
	private String mode;

	@Column(nullable = false, length = 20)
	private String status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected BackTeacherSubjectTransferRequestEntity() {
	}

	public BackTeacherSubjectTransferRequestEntity(
			long subjectId, long fromUserId, long toUserId, String mode) {
		this.subjectId = subjectId;
		this.fromUserId = fromUserId;
		this.toUserId = toUserId;
		this.mode = mode;
		this.status = "pending";
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public long getSubjectId() {
		return subjectId;
	}

	public long getFromUserId() {
		return fromUserId;
	}

	public long getToUserId() {
		return toUserId;
	}

	public String getMode() {
		return mode;
	}

	public String getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void accept() {
		this.status = "accepted";
	}

	public void reject() {
		this.status = "rejected";
	}

	public void cancel() {
		this.status = "cancelled";
	}
}
