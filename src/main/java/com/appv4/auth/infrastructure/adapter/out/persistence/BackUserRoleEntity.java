package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Ligne {@code back_user_roles} — un rôle déjà accordé (parent, teacher, admin).
 *
 * <p><b>Business :</b> à l'inscription, seule la ligne {@code parent} peut naître ici.
 * {@code teacher} n'arrive qu'après validation admin.
 */
@Entity
@Table(
		name = "back_user_roles",
		uniqueConstraints = @UniqueConstraint(
				name = "uq_back_user_roles_user_role",
				columnNames = { "user_id", "role" }))
public class BackUserRoleEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private long userId;

	@Column(nullable = false, length = 32)
	private String role;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected BackUserRoleEntity() {
	}

	public BackUserRoleEntity(long userId, String role) {
		this.userId = userId;
		this.role = role;
	}

	public String getRole() {
		return role;
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
