package com.appv4.auth.application.port.out;

import java.time.LocalDate;
import java.util.Map;

/**
 * Niveaux de badges déjà atteints (BACK-19).
 *
 * <p><b>Business :</b> un niveau gardé ne redescend pas. Pas de ligne veut dire niveau 0.
 * <p><b>Technique :</b> {@code back_child_badges} et {@code back_child_badge_inits}.
 * <p><b>Métier / flow :</b> lecture et mise à jour après un essai.
 */
public interface ChildBadgePort {

	boolean initialized(long childId);

	void markInitialized(long childId);

	Map<String, StoredBadge> load(long childId);

	void save(long childId, StoredBadge badge);

	record StoredBadge(String code, int level, int retainedValue, LocalDate unlockedOn) {
	}
}
