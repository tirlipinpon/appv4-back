package com.appv4.ai.infrastructure.adapter.out.persistence;

import java.util.List;

import com.appv4.ai.application.port.out.ChangementEnregistre;
import com.appv4.ai.application.port.out.HistoriqueBesoinPort;

/**
 * Historique des paires en base (BACK-32).
 *
 * <p><b>Business :</b> le plus récent est lu en premier.
 * <p><b>Technique :</b> table {@code back_ai_route_changes}.
 * <p><b>Métier / flow :</b> adaptateur out après un changement accepté.
 */
public class HistoriqueBesoinPersistenceAdapter implements HistoriqueBesoinPort {

	private final BackAiRouteChangeJpaRepository changements;

	public HistoriqueBesoinPersistenceAdapter(BackAiRouteChangeJpaRepository changements) {
		this.changements = changements;
	}

	@Override
	public void ajouter(ChangementEnregistre changement) {
		changements.save(new BackAiRouteChangeEntity(
				changement.code(),
				changement.nom(),
				changement.nomAdmin(),
				changement.instant(),
				changement.ancienFournisseur(),
				changement.ancienModele(),
				changement.nouveauFournisseur(),
				changement.nouveauModele()));
	}

	@Override
	public List<ChangementEnregistre> tous() {
		return changements.findAllByOrderByChangedAtDesc().stream()
				.map(ligne -> new ChangementEnregistre(
						ligne.getBesoinCode(),
						ligne.getLabel(),
						ligne.getAdminDisplayName(),
						ligne.getChangedAt(),
						ligne.getOldProvider(),
						ligne.getOldModel(),
						ligne.getNewProvider(),
						ligne.getNewModel()))
				.toList();
	}
}
