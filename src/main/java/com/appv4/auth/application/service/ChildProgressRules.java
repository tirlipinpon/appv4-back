package com.appv4.auth.application.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.appv4.auth.application.port.out.ChildProgressPort.StoredProgress;

/**
 * Règles figées XP / streak / palier / semaine (BACK-17).
 *
 * <p><b>Business :</b> le front n'invente aucun calcul ; tout passe ici.
 * <p><b>Technique :</b> fonctions pures + date du jour injectée par le service.
 * <p><b>Métier / flow :</b> après attempt → {@link #afterAttempt} ; GET → {@link #toView}.
 */
public final class ChildProgressRules {

	/** XP gagnés = score / 10 (100 % → 10 XP, 0 % → 0). */
	public static final int XP_DIVISOR = 10;

	private ChildProgressRules() {
	}

	public static int xpGain(int scorePercent) {
		if (scorePercent <= 0) {
			return 0;
		}
		return Math.min(100, scorePercent) / XP_DIVISOR;
	}

	public static String leagueLabel(int xpTotal) {
		if (xpTotal >= 350) {
			return "Diamant";
		}
		if (xpTotal >= 150) {
			return "Or";
		}
		if (xpTotal >= 50) {
			return "Argent";
		}
		return "Bronze";
	}

	public static LocalDate mondayOf(LocalDate day) {
		return day.with(DayOfWeek.MONDAY);
	}

	public static int dayIndexMonFirst(LocalDate day) {
		return day.getDayOfWeek().getValue() - 1;
	}

	public static StoredProgress empty(long childId) {
		return new StoredProgress(childId, 0, 0, null, null, "0000000");
	}

	/**
	 * Met à jour XP / streak / semaine après une tentative acceptée par le back.
	 */
	public static StoredProgress afterAttempt(StoredProgress current, int scorePercent, LocalDate today) {
		int xp = current.xpTotal() + xpGain(scorePercent);
		int streak = nextStreak(current.currentStreak(), current.lastPlayDate(), today);
		LocalDate weekStart = mondayOf(today);
		String weekDays = markPlayed(current.weekStartDate(), current.weekDays(), weekStart, today);
		return new StoredProgress(current.childId(), xp, streak, today, weekStart, weekDays);
	}

	static int nextStreak(int currentStreak, LocalDate lastPlayDate, LocalDate today) {
		if (lastPlayDate != null && lastPlayDate.equals(today)) {
			return Math.max(currentStreak, 1);
		}
		if (lastPlayDate != null && lastPlayDate.equals(today.minusDays(1))) {
			return Math.max(currentStreak, 0) + 1;
		}
		return 1;
	}

	static String markPlayed(
			LocalDate storedWeekStart, String storedWeekDays, LocalDate weekStart, LocalDate today) {
		String base = (storedWeekStart != null && storedWeekStart.equals(weekStart))
				? normalizeWeek(storedWeekDays)
				: "0000000";
		char[] chars = base.toCharArray();
		chars[dayIndexMonFirst(today)] = '1';
		return new String(chars);
	}

	/**
	 * Streak affiché : cassé si le dernier jour joué est avant hier.
	 */
	public static int visibleStreak(int storedStreak, LocalDate lastPlayDate, LocalDate today) {
		if (lastPlayDate == null) {
			return 0;
		}
		if (lastPlayDate.isBefore(today.minusDays(1))) {
			return 0;
		}
		return Math.max(storedStreak, 0);
	}

	/**
	 * Nombre d'étoiles jaunes gagnées chaque jour de la semaine en cours.
	 *
	 * <p><b>Business :</b> un jour déjà commencé montre un nombre, même zéro.
	 * Un jour encore à venir n'a pas de nombre. Une question rejouée le même jour compte 1.
	 * <p><b>Technique :</b> {@code null} pour un jour futur. Les dates sont déjà en Europe/Paris.
	 * <p><b>Métier / flow :</b> GET progress, après la lecture des premières étoiles jaunes.
	 */
	public static List<Integer> weekYellowCounts(List<YellowGain> gains, LocalDate today) {
		LocalDate monday = mondayOf(today);
		List<Integer> counts = new ArrayList<>(7);
		for (int i = 0; i < 7; i++) {
			LocalDate day = monday.plusDays(i);
			if (day.isAfter(today)) {
				counts.add(null);
				continue;
			}
			Set<Long> questions = new HashSet<>();
			if (gains != null) {
				for (YellowGain gain : gains) {
					if (gain != null && day.equals(gain.day())) {
						questions.add(gain.questionId());
					}
				}
			}
			counts.add(questions.size());
		}
		return Collections.unmodifiableList(counts);
	}

	public record YellowGain(long questionId, LocalDate day) {
	}

	public static List<Boolean> weekDaysForDisplay(
			LocalDate storedWeekStart, String storedWeekDays, LocalDate today) {
		LocalDate weekStart = mondayOf(today);
		String bits = (storedWeekStart != null && storedWeekStart.equals(weekStart))
				? normalizeWeek(storedWeekDays)
				: "0000000";
		List<Boolean> days = new ArrayList<>(7);
		for (int i = 0; i < 7; i++) {
			days.add(bits.charAt(i) == '1');
		}
		return List.copyOf(days);
	}

	public static ChildProgressViewSnapshot toView(StoredProgress stored, LocalDate today) {
		return new ChildProgressViewSnapshot(
				stored.xpTotal(),
				visibleStreak(stored.currentStreak(), stored.lastPlayDate(), today),
				leagueLabel(stored.xpTotal()),
				weekDaysForDisplay(stored.weekStartDate(), stored.weekDays(), today));
	}

	private static String normalizeWeek(String weekDays) {
		if (weekDays == null || weekDays.length() != 7 || !weekDays.chars().allMatch(c -> c == '0' || c == '1')) {
			return "0000000";
		}
		return weekDays;
	}

	public record ChildProgressViewSnapshot(
			int xpTotal, int currentStreak, String leagueLabel, List<Boolean> weekDays) {
	}
}
