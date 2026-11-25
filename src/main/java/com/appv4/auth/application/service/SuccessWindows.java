package com.appv4.auth.application.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Réussite comparée sur deux fenêtres de 7×24 h.
 *
 * <p><b>Business :</b> la même tendance décide la demande d'humeur et la lecture parent.
 * <p><b>Technique :</b> l'écart est en millièmes de point. L'essai pile sur la coupure
 * reste dans la fenêtre la plus ancienne.
 * <p><b>Métier / flow :</b> appelé par la demande d'humeur et par le suivi parent.
 */
final class SuccessWindows {

	static final Duration WEEK = Duration.ofHours(7 * 24);
	private static final long THOUSANDTHS = 100_000L;
	private static final long EIGHT_POINTS = 8_000L;

	private SuccessWindows() {
	}

	record Sample(Instant at, boolean passed) {
	}

	record Result(boolean present, String direction, BigDecimal delta) {
	}

	static Result at(List<Sample> samples, Instant now) {
		Instant cut = now.minus(WEEK);
		Instant older = cut.minus(WEEK);
		int recentPassed = 0;
		int recentTotal = 0;
		int previousPassed = 0;
		int previousTotal = 0;
		if (samples != null) {
			for (Sample sample : samples) {
				Instant at = sample.at();
				if (at == null || at.isAfter(now)) {
					continue;
				}
				if (at.isAfter(cut)) {
					recentTotal++;
					if (sample.passed()) {
						recentPassed++;
					}
				} else if (at.isAfter(older)) {
					previousTotal++;
					if (sample.passed()) {
						previousPassed++;
					}
				}
			}
		}
		if (recentTotal == 0 || previousTotal == 0) {
			return new Result(false, null, null);
		}
		long delta = points(recentPassed, recentTotal) - points(previousPassed, previousTotal);
		String direction = direction(delta);
		return new Result(true, direction, shown(delta));
	}

	static long rate(int passed, int total) {
		return points(passed, total);
	}

	private static long points(int passed, int total) {
		return passed * THOUSANDTHS / total;
	}

	private static String direction(long delta) {
		if (delta <= -EIGHT_POINTS) {
			return "baisse";
		}
		if (delta >= EIGHT_POINTS) {
			return "hausse";
		}
		return "stable";
	}

	/** Dixième coupé vers zéro, pour qu'un stable ne s'affiche pas sur le seuil. */
	private static BigDecimal shown(long deltaThousandths) {
		return BigDecimal.valueOf(deltaThousandths / 100, 1);
	}
}
