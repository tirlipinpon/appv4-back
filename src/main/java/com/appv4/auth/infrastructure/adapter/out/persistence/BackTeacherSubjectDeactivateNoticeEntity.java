package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Alerte dismissible après désactivation par le créateur.
 *
 * <p><b>Business :</b> les co-profs qui ont la matière sont prévenus de qui
 * a été désactivé (le créateur et les collègues choisis).
 * <p><b>Technique :</b> table {@code back_teacher_subject_deactivate_notices}.
 * <p><b>Métier / flow :</b> créée après POST deactivate du propriétaire → listée
 * puis dismissée par le destinataire.
 */
@Entity
@Table(name = "back_teacher_subject_deactivate_notices")
public class BackTeacherSubjectDeactivateNoticeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "subject_id", nullable = false)
	private long subjectId;

	@Column(name = "from_user_id", nullable = false)
	private long fromUserId;

	@Column(name = "to_user_id", nullable = false)
	private long toUserId;

	@Column(name = "deactivated_user_ids", nullable = false, length = 500)
	private String deactivatedUserIds;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "dismissed_at")
	private Instant dismissedAt;

	protected BackTeacherSubjectDeactivateNoticeEntity() {
	}

	public BackTeacherSubjectDeactivateNoticeEntity(
			long subjectId, long fromUserId, long toUserId, String deactivatedUserIds) {
		this.subjectId = subjectId;
		this.fromUserId = fromUserId;
		this.toUserId = toUserId;
		this.deactivatedUserIds = deactivatedUserIds;
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

	public String getDeactivatedUserIds() {
		return deactivatedUserIds;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getDismissedAt() {
		return dismissedAt;
	}

	public boolean isOpen() {
		return dismissedAt == null;
	}

	public void dismiss() {
		this.dismissedAt = Instant.now();
	}
}
