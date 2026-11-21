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
public record ChildBadgesView(List<Badge> badges, List<Gained> gainedToday, String notice) {

	public ChildBadgesView {
		badges = List.copyOf(badges);
		gainedToday = List.copyOf(gainedToday);
	}

	public record Badge(
			String code,
			String name,
			int level,
			int retainedValue,
			Integer nextThreshold,
			LocalDate unlockedOn) {
	}

	public record Gained(String code, String name, int level) {
	}
}
