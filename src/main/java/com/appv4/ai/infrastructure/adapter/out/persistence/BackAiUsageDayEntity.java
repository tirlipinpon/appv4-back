package com.appv4.ai.infrastructure.adapter.out.persistence;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/**
 * Cumul des tokens d'un jour UTC (BACK-31).
 *
 * <p><b>Business :</b> le niveau déjà compté ne change pas de modèle après coup.
 * <p><b>Technique :</b> table {@code back_ai_usage_days}.
 */
@Entity
@Table(name = "back_ai_usage_days")
@IdClass(UsageDayKey.class)
public class BackAiUsageDayEntity {

	@Id
	@Column(name = "usage_date", nullable = false)
	private LocalDate usageDate;

	@Id
	@Column(name = "besoin_code", nullable = false, length = 64)
	private String besoinCode;

	@Id
	@Column(nullable = false, length = 64)
	private String provider;

	@Id
	@Column(nullable = false, length = 120)
	private String model;

	@Column(nullable = false)
	private int calls;

	@Column(nullable = false)
	private int tokens;

	protected BackAiUsageDayEntity() {
	}

	public BackAiUsageDayEntity(LocalDate usageDate, String besoinCode, String provider, String model) {
		this.usageDate = usageDate;
		this.besoinCode = besoinCode;
		this.provider = provider;
		this.model = model;
	}

	public void add(int tokensAjoutes) {
		this.calls = this.calls + 1;
		this.tokens = this.tokens + tokensAjoutes;
	}

	public LocalDate getUsageDate() {
		return usageDate;
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

	public int getCalls() {
		return calls;
	}

	public int getTokens() {
		return tokens;
	}
}
