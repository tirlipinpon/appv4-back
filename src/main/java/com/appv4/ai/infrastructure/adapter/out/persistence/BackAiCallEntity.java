package com.appv4.ai.infrastructure.adapter.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Un appel de besoin déjà fait (BACK-31).
 *
 * <p><b>Business :</b> on sait quel modèle a répondu, pas ce qui a été dit.
 * <p><b>Technique :</b> table {@code back_ai_calls}. Aucune colonne de prompt.
 */
@Entity
@Table(name = "back_ai_calls")
public class BackAiCallEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "besoin_code", nullable = false, length = 64)
	private String besoinCode;

	@Column(nullable = false, length = 64)
	private String provider;

	@Column(nullable = false, length = 120)
	private String model;

	@Column(name = "prompt_tokens", nullable = false)
	private int promptTokens;

	@Column(name = "completion_tokens", nullable = false)
	private int completionTokens;

	@Column(name = "total_tokens", nullable = false)
	private int totalTokens;

	@Column(name = "usage_reported", nullable = false)
	private boolean usageReported;

	@Column(nullable = false, length = 16)
	private String status;

	@Column(name = "duration_millis", nullable = false)
	private long durationMillis;

	@Column(name = "called_at", nullable = false)
	private Instant calledAt;

	protected BackAiCallEntity() {
	}

	public BackAiCallEntity(
			String besoinCode,
			String provider,
			String model,
			int promptTokens,
			int completionTokens,
			int totalTokens,
			boolean usageReported,
			String status,
			long durationMillis,
			Instant calledAt) {
		this.besoinCode = besoinCode;
		this.provider = provider;
		this.model = model;
		this.promptTokens = promptTokens;
		this.completionTokens = completionTokens;
		this.totalTokens = totalTokens;
		this.usageReported = usageReported;
		this.status = status;
		this.durationMillis = durationMillis;
		this.calledAt = calledAt;
	}

	public String getBesoinCode() {
		return besoinCode;
	}

	public String getProvider() {
		return provider;
	}

	public String getModel() {
		return model;
	}

	public int getPromptTokens() {
		return promptTokens;
	}

	public int getCompletionTokens() {
		return completionTokens;
	}

	public int getTotalTokens() {
		return totalTokens;
	}

	public boolean isUsageReported() {
		return usageReported;
	}

	public String getStatus() {
		return status;
	}

	public long getDurationMillis() {
		return durationMillis;
	}

	public Instant getCalledAt() {
		return calledAt;
	}
}
