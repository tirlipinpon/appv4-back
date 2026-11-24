package com.appv4.ai.infrastructure.adapter.out.persistence;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Clé du cumul du jour (BACK-31).
 *
 * <p><b>Business :</b> un jour, un besoin, un fournisseur, un modèle.
 * <p><b>Technique :</b> le jour est une date UTC.
 */
public class UsageDayKey implements Serializable {

	private LocalDate usageDate;
	private String besoinCode;
	private String provider;
	private String model;

	public UsageDayKey() {
	}

	public UsageDayKey(LocalDate usageDate, String besoinCode, String provider, String model) {
		this.usageDate = usageDate;
		this.besoinCode = besoinCode;
		this.provider = provider;
		this.model = model;
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

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof UsageDayKey key)) {
			return false;
		}
		return Objects.equals(usageDate, key.usageDate)
				&& Objects.equals(besoinCode, key.besoinCode)
				&& Objects.equals(provider, key.provider)
				&& Objects.equals(model, key.model);
	}

	@Override
	public int hashCode() {
		return Objects.hash(usageDate, besoinCode, provider, model);
	}
}
