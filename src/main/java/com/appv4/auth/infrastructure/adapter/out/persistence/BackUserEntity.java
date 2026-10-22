package com.appv4.auth.infrastructure.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Mapping JPA de la table {@code back_users}.
 *
 * <p><b>Métier :</b> un compte admin (parent/prof). Pas encore de signup ici :
 * la table est créée pour les prochains tickets ; Hibernate valide le schéma.
 *
 * <p><b>Important :</b> ce n'est PAS un objet du domaine hexagonal — c'est un
 * adaptateur persistence. Le domaine aura un modèle {@code User} plus tard.
 */
@Entity
@Table(name = "back_users")
public class BackUserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	/** Hash BCrypt/Argon2 — jamais le mot de passe en clair. NULL si compte social pur. */
	@Column(name = "password_hash", length = 255)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 255)
	private String displayName;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	/** Dernier espace ouvert : {@code parent}, {@code teacher}, ou null (SCRUM-124). */
	@Column(name = "last_active_role", length = 32)
	private String lastActiveRole;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getLastActiveRole() {
		return lastActiveRole;
	}

	public void setLastActiveRole(String lastActiveRole) {
		this.lastActiveRole = lastActiveRole;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
