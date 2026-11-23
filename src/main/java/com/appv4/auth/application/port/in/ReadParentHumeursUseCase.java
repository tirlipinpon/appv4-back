package com.appv4.auth.application.port.in;

import java.util.List;

/**
 * Dernière humeur de chaque enfant, pour le parent (BACK-71).
 *
 * <p><b>Business :</b> le parent lit la réponse déjà gardée, avec la date et l'heure.
 * Un enfant sans réponse n'a pas d'humeur d'exemple.
 * <p><b>Technique :</b> même liste que Mes enfants. Les libellés sont déjà calculés.
 * <p><b>Métier / flow :</b> page Humeur → ce port. Ce n'est pas la route progress.
 */
public interface ReadParentHumeursUseCase {

	List<ParentChildHumeur> list(long parentUserId, String activeRole);

	/**
	 * @param humeur absente s'il n'y a jamais eu de réponse
	 */
	record ParentChildHumeur(long id, String firstName, String lastName, Humeur humeur) {
	}

	/**
	 * @param icon absent si le choix n'en avait pas, ou si c'est une fermeture
	 * @param word absent si le choix n'en avait pas, ou si c'est une fermeture
	 * @param label présent seulement pour une fermeture
	 */
	record Humeur(String icon, String word, String label, String dateLabel, String timeLabel) {
	}
}
