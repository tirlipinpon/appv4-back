package com.appv4.ai.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Route d'un besoin vers un modèle (BACK-30).
 *
 * <p><b>Business :</b> un besoin inactif ne part pas. La ligne reste.
 * <p><b>Technique :</b> table {@code back_ai_task_routes}. La clé du fournisseur n'est pas ici.
 */
@Entity
@Table(name = "back_ai_task_routes")
public class BackAiTaskRouteEntity {

	@Id
	@Column(length = 64)
	private String code;

	@Column(nullable = false, length = 120)
	private String label;

	@Column(nullable = false, length = 64)
	private String provider;

	@Column(nullable = false, length = 120)
	private String model;

	@Column(nullable = false)
	private boolean active = true;

	protected BackAiTaskRouteEntity() {
	}

	public BackAiTaskRouteEntity(String code, String label, String provider, String model, boolean active) {
		this.code = code;
		this.label = label;
		this.provider = provider;
		this.model = model;
		this.active = active;
	}

	public void replace(String label, String provider, String model, boolean active) {
		this.label = label;
		this.provider = provider;
		this.model = model;
		this.active = active;
	}

	public String getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}

	public String getProvider() {
		return provider;
	}

	public String getModel() {
		return model;
	}

	public boolean isActive() {
		return active;
	}
}
