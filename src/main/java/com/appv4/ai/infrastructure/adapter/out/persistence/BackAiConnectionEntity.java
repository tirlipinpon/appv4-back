package com.appv4.ai.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Connexion gardée pour un essai (BACK-84).
 *
 * <p><b>Business :</b> la clé reste en base. L'admin ne la relit pas.
 * <p><b>Technique :</b> table {@code back_ai_connections}.
 */
@Entity
@Table(name = "back_ai_connections")
public class BackAiConnectionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 64)
	private String provider;

	@Column(nullable = false, length = 120)
	private String model;

	@Column(name = "api_key", nullable = false, length = 500)
	private String apiKey;

	protected BackAiConnectionEntity() {
	}

	public BackAiConnectionEntity(String provider, String model, String apiKey) {
		this.provider = provider;
		this.model = model;
		this.apiKey = apiKey;
	}

	public Long getId() {
		return id;
	}

	public String getProvider() {
		return provider;
	}

	public String getModel() {
		return model;
	}

	public String getApiKey() {
		return apiKey;
	}
}
