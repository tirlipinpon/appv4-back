package com.appv4.auth.application.result;

import java.time.LocalDate;
import java.util.List;

/**
 * Collection renvoyée à l'enfant (BACK-19).
 *
 * <p><b>Business :</b> neuf badges, dans un ordre fixe. Le bloc du jour ne contient
 * que les niveaux franchis aujourd'hui. L'avertissement de matière est absent tant
 * que ce badge n'est pas calculé.
 * <p><b>Technique :</b> record avant le JSON.
 * <p><b>Métier / flow :</b> lecture des badges → accueil et Récompenses.
 */
public record ChildBadgesView(List<Badge> badges, List<Gained> gainedToday, Notice notice) {

	public ChildBadgesView {
		badges = List.copyOf(badges);
		gainedToday = List.copyOf(gainedToday);
	}

	/**
	 * Une carte de la collection.
	 *
	 * <p><b>Business :</b> {@code retainedValue} est le record qui a donné le niveau.
	 * {@code progressValue} remplit la jauge : la série en cours pour Bonnes réponses
	 * d'affilée, le record pour les autres badges.
	 * <p><b>Technique :</b> les deux nombres partent dans le JSON. La jauge ne recalcule pas.
	 */
	public record Badge(
			String code,
			String name,
			int level,
			int retainedValue,
			Integer nextThreshold,
			LocalDate unlockedOn,
			int progressValue) {
	}

	public record Gained(String code, String name, int level) {
	}

	/** Questions encore à jaunir sur la matière qui a donné le badge. */
	public record Notice(String label, String subjectName, int remainingCount) {
	}
}
