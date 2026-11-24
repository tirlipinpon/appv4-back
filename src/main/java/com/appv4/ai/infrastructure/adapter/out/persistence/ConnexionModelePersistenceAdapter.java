package com.appv4.ai.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import com.appv4.ai.application.ConnexionModele;
import com.appv4.ai.application.port.out.ConnexionModelePort;

/**
 * Connexions en base (BACK-84).
 *
 * <p><b>Business :</b> la liste n'emporte pas la clé.
 * <p><b>Technique :</b> la clé n'est lue que pour {@link #trouver}.
 * <p><b>Métier / flow :</b> adaptateur out de l'essai de connexion.
 */
public class ConnexionModelePersistenceAdapter implements ConnexionModelePort {

	private final BackAiConnectionJpaRepository connexions;

	public ConnexionModelePersistenceAdapter(BackAiConnectionJpaRepository connexions) {
		this.connexions = connexions;
	}

	@Override
	public ConnexionModele enregistrer(String fournisseur, String modele, String cle) {
		BackAiConnectionEntity saved = connexions.save(new BackAiConnectionEntity(fournisseur, modele, cle));
		return new ConnexionModele(saved.getId(), saved.getProvider(), saved.getModel());
	}

	@Override
	public List<ConnexionModele> lister() {
		return connexions.findAll().stream()
				.map(row -> new ConnexionModele(row.getId(), row.getProvider(), row.getModel()))
				.toList();
	}

	@Override
	public Optional<Secrete> trouver(long id) {
		return connexions.findById(id)
				.map(row -> new Secrete(row.getId(), row.getProvider(), row.getModel(), row.getApiKey()));
	}
}
