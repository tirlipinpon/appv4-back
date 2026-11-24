package com.appv4.ai.application.port.out;

import java.util.List;

import com.appv4.ai.application.AppelEnregistre;
import com.appv4.ai.application.CumulJour;

/**
 * Les appels d'un besoin et leur cumul du jour (BACK-31).
 *
 * <p><b>Business :</b> un appel raté compte, sans nombre de tokens inventé.
 * <p><b>Technique :</b> l'appel et le cumul du même geste sont une seule écriture.
 * <p><b>Métier / flow :</b> le routeur note après l'adaptateur. L'essai de connexion n'arrive pas ici.
 */
public interface UsageBesoinPort {

	void noter(AppelEnregistre appel);

	List<AppelEnregistre> appels();

	List<CumulJour> cumuls();
}
