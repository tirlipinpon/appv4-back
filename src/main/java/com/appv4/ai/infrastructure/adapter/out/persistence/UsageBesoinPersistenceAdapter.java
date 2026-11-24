package com.appv4.ai.infrastructure.adapter.out.persistence;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.ai.application.AppelEnregistre;
import com.appv4.ai.application.CumulJour;
import com.appv4.ai.application.port.out.UsageBesoinPort;

/**
 * Appels et cumuls du jour en base (BACK-31).
 *
 * <p><b>Business :</b> l'appel et le jour bougent ensemble.
 * <p><b>Technique :</b> une transaction : insert de l'appel, puis ajout sur la ligne du jour.
 * <p><b>Métier / flow :</b> adaptateur out appelé par le routeur, pas par une page.
 */
public class UsageBesoinPersistenceAdapter implements UsageBesoinPort {

	private final BackAiCallJpaRepository calls;
	private final BackAiUsageDayJpaRepository days;

	public UsageBesoinPersistenceAdapter(BackAiCallJpaRepository calls, BackAiUsageDayJpaRepository days) {
		this.calls = calls;
		this.days = days;
	}

	@Override
	@Transactional
	public void noter(AppelEnregistre appel) {
		calls.save(new BackAiCallEntity(
				appel.codeBesoin(),
				appel.fournisseur(),
				appel.modele(),
				appel.tokensPrompt(),
				appel.tokensReponse(),
				appel.tokensTotal(),
				appel.usageSignale(),
				appel.statut(),
				appel.dureeMillis(),
				appel.instant()));
		BackAiUsageDayEntity day = days.findById(new UsageDayKey(
				appel.jourUtc(), appel.codeBesoin(), appel.fournisseur(), appel.modele()))
				.orElseGet(() -> new BackAiUsageDayEntity(
						appel.jourUtc(), appel.codeBesoin(), appel.fournisseur(), appel.modele()));
		day.add(appel.tokensTotal());
		days.save(day);
	}

	@Override
	public List<AppelEnregistre> appels() {
		return calls.findAllByOrderByIdAsc().stream().map(this::toAppel).toList();
	}

	@Override
	public List<CumulJour> cumuls() {
		return days.findAll().stream().map(this::toCumul).toList();
	}

	private AppelEnregistre toAppel(BackAiCallEntity entity) {
		return new AppelEnregistre(
				entity.getBesoinCode(),
				entity.getProvider(),
				entity.getModel(),
				entity.getCalledAt().atZone(java.time.ZoneOffset.UTC).toLocalDate(),
				entity.getPromptTokens(),
				entity.getCompletionTokens(),
				entity.getTotalTokens(),
				entity.isUsageReported(),
				entity.getStatus(),
				entity.getDurationMillis(),
				entity.getCalledAt());
	}

	private CumulJour toCumul(BackAiUsageDayEntity entity) {
		return new CumulJour(
				entity.getUsageDate(),
				entity.getBesoinCode(),
				entity.getProvider(),
				entity.getModel(),
				entity.getCalls(),
				entity.getTokens());
	}
}
