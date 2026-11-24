package com.appv4.ai.infrastructure.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un changement de paire déjà accepté (BACK-32).
 *
 * <p><b>Business :</b> l'ancienne paire reste lisible. Le nom de l'admin est celui du compte au moment du geste.
 * <p><b>Technique :</b> table {@code back_ai_route_changes}. Pas de prompt, pas de réponse.
 */
@Entity
@Table(name = "back_ai_route_changes")
public class BackAiRouteChangeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "besoin_code", nullable = false, length = 64)
	private String besoinCode;

	@Column(nullable = false, length = 120)
	private String label;

	@Column(name = "admin_display_name", nullable = false, length = 255)
	private String adminDisplayName;

	@Column(name = "changed_at", nullable = false)
	private Instant changedAt;

	@Column(name = "old_provider", nullable = false, length = 64)
	private String oldProvider;

	@Column(name = "old_model", nullable = false, length = 120)
	private String oldModel;

	@Column(name = "new_provider", nullable = false, length = 64)
	private String newProvider;

	@Column(name = "new_model", nullable = false, length = 120)
	private String newModel;

	protected BackAiRouteChangeEntity() {
	}

	public BackAiRouteChangeEntity(
			String besoinCode,
			String label,
			String adminDisplayName,
			Instant changedAt,
			String oldProvider,
			String oldModel,
			String newProvider,
			String newModel) {
		this.besoinCode = besoinCode;
		this.label = label;
		this.adminDisplayName = adminDisplayName;
		this.changedAt = changedAt;
		this.oldProvider = oldProvider;
		this.oldModel = oldModel;
		this.newProvider = newProvider;
		this.newModel = newModel;
	}

	public String getBesoinCode() {
		return besoinCode;
	}

	public String getLabel() {
		return label;
	}

	public String getAdminDisplayName() {
		return adminDisplayName;
	}

	public Instant getChangedAt() {
		return changedAt;
	}

	public String getOldProvider() {
		return oldProvider;
	}

	public String getOldModel() {
		return oldModel;
	}

	public String getNewProvider() {
		return newProvider;
	}

	public String getNewModel() {
		return newModel;
	}
}
