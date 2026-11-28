package com.appv4.auth.application.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bonus d'XP quand la mission du jour est sans faute.
 *
 * <p><b>Business :</b> le bonus s'ajoute aux XP déjà gagnés. Il part du même
 * montant que la mission (10 XP par question), puis monte d'une moitié à chaque
 * mission sans faute d'affilée. Un jour sauté remet la suivante au départ.
	 * <p><b>Technique :</b> {@code (2 + jours d'affilée) * 5 * questions}. Pour 3 questions :
 * 30, 45, 60, 75.
 * <p><b>Métier / flow :</b> lecture de la mission et clôture sans faute.
 */
public final class SansFauteRules {

	private SansFauteRules() {
	}

	public static int bonusXp(int questionCount, int previousStreak) {
		if (questionCount <= 0 || previousStreak < 0) {
			return 0;
		}
		return (2 + previousStreak) * 5 * questionCount;
	}

	/** Missions sans faute collées à hier, sans compter aujourd'hui. */
	public static int streakBefore(List<LocalDate> sansFauteDays, LocalDate today) {
		Set<LocalDate> days = sansFauteDays == null ? Set.of() : new HashSet<>(sansFauteDays);
		int streak = 0;
		LocalDate day = today.minusDays(1);
		while (days.contains(day)) {
			streak++;
			day = day.minusDays(1);
		}
		return streak;
	}
}
