package com.appv4.ai.application.port.out;

import java.util.List;

/**
 * Historique des paires d'un besoin (BACK-32).
 *
 * <p><b>Business :</b> désactiver ne l'efface pas.
 * <p><b>Technique :</b> l'ordre de lecture est du plus récent au plus ancien.
 * <p><b>Métier / flow :</b> port out écrit après un changement accepté.
 */
public interface HistoriqueBesoinPort {

	void ajouter(ChangementEnregistre changement);

	List<ChangementEnregistre> tous();
}
