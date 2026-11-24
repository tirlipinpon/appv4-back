package com.appv4.ai.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Les routes des besoins (BACK-30).
 *
 * <p><b>Business :</b> une ligne dit quel modèle répond pour un besoin.
 * <p><b>Technique :</b> vide au premier démarrage : le service pose alors les routes de la config.
 * <p><b>Métier / flow :</b> port out du routeur, avant l'adaptateur du fournisseur.
 */
public interface RouteBesoinPort {

	Optional<RouteBesoin> trouver(String code);

	List<RouteBesoin> toutes();

	void enregistrer(RouteBesoin route);
}
