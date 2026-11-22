package com.appv4.auth.application.port.out;

import java.time.Instant;
import java.util.List;

/**
 * Copies lisibles pour proposer un centre d'intérêt.
 *
 * <p><b>Business :</b> une suggestion ne dit pas à qui appartient le mot.
 * Un enfant désactivé ou retiré de l'affichage ne compte pas.
 * <p><b>Technique :</b> une ligne par copie, avec le parent de la fiche.
 * <p><b>Métier / flow :</b> lecture de {@code GET /api/int/parent/interest-suggestions}.
 */
public interface ChildInterestSuggestionPort {

	List<SuggestableInterest> findAll();

	/**
	 * @param active faux : l'enfant est désactivé, sa copie ne compte pas
	 * @param hidden vrai : l'enfant est retiré de l'affichage, sa copie ne compte pas
	 */
	record SuggestableInterest(
			long id,
			long parentUserId,
			String text,
			Instant writtenAt,
			boolean active,
			boolean hidden) {
	}
}
