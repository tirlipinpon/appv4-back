package com.appv4.auth.application.port.out;

import java.time.Instant;
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
