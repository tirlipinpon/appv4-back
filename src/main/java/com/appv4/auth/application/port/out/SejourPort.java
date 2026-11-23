package com.appv4.auth.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Séjours déjà gardés (BACK-38).
 *
 * <p><b>Business :</b> un enfant a au plus un séjour ouvert. Le temps fermé est un
 * intervalle, pas une ligne par seconde. La tranche ouverte se relit telle quelle.
 * <p><b>Technique :</b> {@code back_sejours} et {@code back_sejour_intervals}.
 * <p><b>Métier / flow :</b> port out du cas d'usage Séjour.
 */
public interface SejourPort {

	Optional<OpenSlice> findOpen(long childId);

	OpenSlice open(long childId, Instant startedAt, String deviceId, String screen);

	void saveOpen(OpenSlice slice);

	void appendInterval(
			long sejourId,
			String screen,
			Long questionId,
			String prompt,
			String subjectName,
			Instant startedAt,
			Instant endedAt);

	void close(long sejourId, Instant closedAt);

	List<StoredSejour> list(long childId);

	/**
	 * Tranche encore ouverte.
	 *
	 * <p><b>Business :</b> l'appareil qui tient le séjour, l'écran ou la question,
	 * le dernier signal confirmé et l'horloge du geste.
	 *
	 * @param questionId question de la tranche, absente sur un écran
	 * @param prompt énoncé copié quand la ligne a atteint une seconde, absent avant
	 * @param subjectName nom de matière copié en même temps que l'énoncé
	 */
	record OpenSlice(
			long id,
			long childId,
			Instant startedAt,
			String deviceId,
			String screen,
			Long questionId,
			String prompt,
			String subjectName,
			Instant sliceStartedAt,
			Instant lastConfirmedAt,
			Instant lastGestureAt) {
	}

	/**
	 * Intervalle déjà fermé. Un écran ou une question, pas les deux.
	 *
	 * @param questionId question jouée, absente pour un écran
	 * @param prompt énoncé copié, absent tant que la ligne n'a pas une seconde
	 * @param subjectName nom de matière copié avec l'énoncé
	 */
	record StoredInterval(
			String screen, Long questionId, String prompt, String subjectName, Instant startedAt, Instant endedAt) {
	}

	/**
	 * Séjour relu pour le parent, ouvert ou fermé.
	 *
	 * @param screen tranche ouverte, absente si le séjour est fermé ou si c'est une question
	 * @param questionId question de la tranche ouverte, absente sur un écran
	 * @param prompt énoncé déjà copié sur la tranche ouverte
	 * @param subjectName nom de matière déjà copié sur la tranche ouverte
	 */
	record StoredSejour(
			long id,
			long childId,
			Instant startedAt,
			boolean open,
			String deviceId,
			String screen,
			Long questionId,
			String prompt,
			String subjectName,
			Instant sliceStartedAt,
			Instant lastConfirmedAt,
			Instant lastGestureAt,
			List<StoredInterval> intervals) {
	}
}
