package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Alerte dismissible liée à une matière (désactivation, handover, …).
 *
 * <p><b>Business :</b> le destinataire lit un message prêt et ferme l'alerte.
 * <p><b>Technique :</b> table {@code back_teacher_subject_deactivate_notices}
 * (nom historique) avec {@code kind} et {@code message}.
 * <p><b>Métier / flow :</b> créée par le service matières → listée puis dismissée.
 */
@Entity
@Table(name = "back_teacher_subject_deactivate_notices")
public class BackTeacherSubjectDeactivateNoticeEntity {

	public static final String KIND_DEACTIVATE = "deactivate";
	public static final String KIND_HANDOVER_ACCEPTED = "handover_accepted";
	public static final String KIND_HANDOVER_REJECTED = "handover_rejected";

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

	@Column(nullable = false, length = 40)
	private String kind = KIND_DEACTIVATE;

	@Column(length = 500)
	private String message;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "dismissed_at")
	private Instant dismissedAt;

	protected BackTeacherSubjectDeactivateNoticeEntity() {
	}

	public BackTeacherSubjectDeactivateNoticeEntity(
			long subjectId, long fromUserId, long toUserId, String deactivatedUserIds) {
		this(subjectId, fromUserId, toUserId, deactivatedUserIds, KIND_DEACTIVATE, null);
	}

	public BackTeacherSubjectDeactivateNoticeEntity(
			long subjectId,
			long fromUserId,
			long toUserId,
			String deactivatedUserIds,
			String kind,
			String message) {
		this.subjectId = subjectId;
		this.fromUserId = fromUserId;
		this.toUserId = toUserId;
		this.deactivatedUserIds = deactivatedUserIds == null ? "" : deactivatedUserIds;
		this.kind = kind == null || kind.isBlank() ? KIND_DEACTIVATE : kind;
		this.message = message;
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

	public String getKind() {
		return kind;
	}

	public String getMessage() {
		return message;
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
