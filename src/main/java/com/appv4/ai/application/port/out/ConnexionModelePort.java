package com.appv4.ai.application.port.out;

import java.util.List;
import java.util.Optional;

import com.appv4.ai.application.ConnexionModele;

/**
 * Connexions enregistrées (BACK-84).
 *
 * <p><b>Business :</b> la clé sert à l'essai, elle ne sort pas dans la liste.
 * <p><b>Technique :</b> {@link Secrete} reste dans le module. L'API ne la reprend pas.
 */
public interface ConnexionModelePort {

	ConnexionModele enregistrer(String fournisseur, String modele, String cle);

	List<ConnexionModele> lister();

	Optional<Secrete> trouver(long id);

	record Secrete(long id, String fournisseur, String modele, String cle) {
	}
}
