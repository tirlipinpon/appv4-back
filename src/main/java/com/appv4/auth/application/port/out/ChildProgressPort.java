package com.appv4.auth.application.port.out;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Persistance de la progression enfant (BACK-17).
 *
 * <p><b>Business :</b> une seule fiche par enfant : XP, streak, jours de la semaine.
 * <p><b>Technique :</b> table {@code back_child_progress}.
 * <p><b>Métier / flow :</b> lecture GET progress + mise à jour après une tentative.
 */
public interface ChildProgressPort {

	Optional<StoredProgress> findByChildId(long childId);

	/** Lock the child's progress until commit, even when its first progress row is still absent. */
	Optional<StoredProgress> findForUpdate(long childId);

	StoredProgress save(StoredProgress progress);

	/**
	 * Ajoute des XP sans toucher à la série ni à la semaine.
	 *
	 * <p><b>Business :</b> le bonus sans faute s'ajoute au total déjà gagné.
	 * <p><b>Technique :</b> relit la fiche, puis la réécrit avec le total augmenté.
	 * <p><b>Métier / flow :</b> une fois, quand la mission du jour se ferme sans faute.
	 */
	default void addXp(long childId, int amount) {
	}

	record StoredProgress(
			long childId,
			int xpTotal,
			int currentStreak,
			LocalDate lastPlayDate,
			LocalDate weekStartDate,
			String weekDays) {
		public StoredProgress {
			weekDays = weekDays == null || weekDays.isBlank() ? "0000000" : weekDays;
		}
	}
}
