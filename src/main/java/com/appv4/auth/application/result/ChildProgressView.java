package com.appv4.auth.application.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Progression affichable sur l'accueil learning (BACK-17).
 *
 * <p><b>Business :</b> XP, série, palier et semaine viennent uniquement du back.
 * <p><b>Technique :</b> record métier avant sérialisation HTTP.
 * <p><b>Métier / flow :</b> {@code ReadChildProgressUseCase} → controller.
 */
public record ChildProgressView(
		int xpTotal,
		int currentStreak,
		String leagueLabel,
		List<Boolean> weekDays,
		List<Integer> weekYellowCounts) {

	public ChildProgressView {
		weekDays = weekDays == null ? List.of() : List.copyOf(weekDays);
		weekYellowCounts = weekYellowCounts == null
				? List.of()
				: Collections.unmodifiableList(new ArrayList<>(weekYellowCounts));
	}
}
