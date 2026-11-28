package com.appv4.auth.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Réponses d'humeur déjà gardées (BACK-69).
 *
 * <p><b>Business :</b> une réponse est un choix copié au moment de l'envoi, ou une fermeture.
 * La dernière sert au délai. Les précédentes restent.
 * <p><b>Technique :</b> lignes de {@code back_humeur_answers}, instant puis identifiant.
 * <p><b>Métier / flow :</b> port out de la demande à l'enfant.
 */
public interface HumeurAnswerPort {

	Optional<StoredAnswer> latest(long childId);

	/**
	 * Toutes les humeurs de l'enfant, de la plus récente à la plus ancienne.
	 *
	 * <p><b>Business :</b> le parent relit aussi les réponses d'avant, fermeture comprise.
	 * <p><b>Technique :</b> {@code answered_at} puis {@code id}, descendant.
	 * <p><b>Métier / flow :</b> GET parent des humeurs.
	 */
	default List<StoredAnswer> history(long childId) {
		return latest(childId).map(List::of).orElseGet(List::of);
	}

	StoredAnswer insert(
			long childId, Long choiceId, String icon, String word, boolean closed, Instant answeredAt);

	/**
	 * @param choiceId absent pour une fermeture
	 * @param icon copie du moment, absente pour une fermeture
	 * @param word copie du moment, absente pour une fermeture
	 */
	record StoredAnswer(
			long id,
			long childId,
			Long choiceId,
			String icon,
			String word,
			boolean closed,
			Instant answeredAt) {
	}
}
