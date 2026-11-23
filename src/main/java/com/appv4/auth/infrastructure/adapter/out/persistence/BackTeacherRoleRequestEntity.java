package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Demande « devenir professeur » (SCRUM-121).
 *
 * <p><b>Business :</b> cocher Professeur n'ouvre pas l'espace teacher.
 * Un admin validera plus tard. Tant que {@code status=pending}, pas de rôle teacher.
 */
@Entity
@Table(name = "back_teacher_role_requests")
public class BackTeacherRoleRequestEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private long userId;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	/** Date du dernier oui. Pas écrasée quand on révoque. */
	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	/** Date du retrait. Null tant que la demande n'est pas révoquée. */
	@Column(name = "revoked_at")
	private Instant revokedAt;

	protected BackTeacherRoleRequestEntity() {
	}

	public BackTeacherRoleRequestEntity(long userId, String status) {
		this.userId = userId;
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public long getUserId() {
		return userId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public void setReviewedAt(Instant reviewedAt) {
		this.reviewedAt = reviewedAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public void setRevokedAt(Instant revokedAt) {
		this.revokedAt = revokedAt;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
