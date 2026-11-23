package com.appv4.auth.application.port.out;

import java.util.List;

/**
 * Lecture des comptes pour la récupération de l'identifiant.
 *
 * <p><b>Business :</b> la personne ne prouve pas être titulaire. On lit le nom affiché,
 * le téléphone et le mail de contact, sans les modifier.
 * <p><b>Technique :</b> une liste de comptes. Pas d'écriture.
 * <p><b>Métier / flow :</b> port out du cas d'usage de récupération, avant le masque.
 */
public interface RecoveryAccountPort {

	List<RecoveryCompte> all();

	/**
	 * @param loginEmail   email de connexion, celui qui peut être masqué
	 * @param active       vrai quand une session peut encore être émise
	 */
	record RecoveryCompte(
			long id,
			String loginEmail,
			String displayName,
			String phone,
			String contactEmail,
			boolean active) {
	}
}
