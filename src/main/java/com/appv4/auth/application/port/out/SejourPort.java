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

	void appendInterval(long sejourId, String screen, Instant startedAt, Instant endedAt);

	void close(long sejourId, Instant closedAt);

	List<StoredSejour> list(long childId);

	/**
	 * Tranche encore ouverte.
	 *
	 * <p><b>Business :</b> l'appareil qui tient le séjour, l'écran, le dernier signal
	 * confirmé et l'horloge du geste.
	 */
	record OpenSlice(
			long id,
			long childId,
			Instant startedAt,
			String deviceId,
			String screen,
			Instant sliceStartedAt,
			Instant lastConfirmedAt,
			Instant lastGestureAt) {
	}

	/**
	 * Intervalle déjà fermé.
	 */
	record StoredInterval(String screen, Instant startedAt, Instant endedAt) {
	}

	/**
	 * Séjour relu pour le parent, ouvert ou fermé.
	 *
	 * @param screen tranche ouverte, absente si le séjour est fermé
	 */
	record StoredSejour(
			long id,
			long childId,
			Instant startedAt,
			boolean open,
			String deviceId,
			String screen,
			Instant sliceStartedAt,
			Instant lastConfirmedAt,
			Instant lastGestureAt,
			List<StoredInterval> intervals) {
	}
}
