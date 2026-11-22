package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Bibliothèque unique des humeurs (BACK-68).
 *
 * <p><b>Business :</b> l'admin propose les mêmes choix à tous les enfants.
 * Une icône absente ou un mot absent n'est pas inventé à la lecture.
 * <p><b>Technique :</b> lignes de {@code back_humeur_choices}, ordre de création.
 * <p><b>Métier / flow :</b> port out de {@link com.appv4.auth.application.port.in.HumeurLibraryUseCase}.
 */
public interface HumeurChoicePort {

	List<HumeurChoice> list();

	HumeurChoice insert(String icon, String word);

	Optional<HumeurChoice> find(long id);

	HumeurChoice update(long id, String icon, String word);

	void delete(long id);

	/**
	 * Un choix déjà enregistré. L'icône ou le mot peut être absent, pas les deux.
	 *
	 * @param icon texte saisi, ou {@code null}
	 * @param word texte saisi, ou {@code null}
	 */
	record HumeurChoice(long id, String icon, String word) {
	}
}
