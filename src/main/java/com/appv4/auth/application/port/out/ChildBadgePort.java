package com.appv4.auth.application.port.out;

import java.time.Instant;
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

	/**
	 * Niveau déjà écrit pour un enfant.
	 *
	 * <p><b>Business :</b> {@code gainedAt} est l'instant du dernier niveau monté.
	 * L'accueil s'en sert pour montrer le gain le plus récent en premier, le même jour.
	 * <p><b>Technique :</b> colonne {@code gained_at}. Null si le niveau n'a pas été daté.
	 */
	record StoredBadge(
			String code,
			int level,
			int retainedValue,
			LocalDate unlockedOn,
			Long subjectId,
			Instant gainedAt) {
	}
}
