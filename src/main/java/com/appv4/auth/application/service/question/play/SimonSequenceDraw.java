package com.appv4.auth.application.service.question.play;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Tire la suite qu'un enfant va regarder puis refaire (BACK-55).
 *
 * <p><b>Business :</b> le professeur pose seulement les éléments. La suite change à chaque
 * ouverture, pour que l'enfant mémorise ce qu'il voit au lieu de réciter une suite apprise.
 * Elle a autant de pas que d'éléments, entre trois et huit. L'écran la montre d'abord sur
 * un pas, puis deux depuis le début, puis trois, jusqu'à la fin. Un élément peut revenir.
 * <p><b>Technique :</b> tirage avec remise de positions 0-based dans la liste des éléments.
 * <p><b>Métier / flow :</b> GET play → ce tirage → {@code ChildSimonChallengePort} garde la suite
 * → POST attempts la note avec {@link SimonChildAnswerGrader}.
 */
public final class SimonSequenceDraw {

	public static final int MIN_STEPS = 3;
	public static final int MAX_STEPS = 8;

	private SimonSequenceDraw() {
	}

	/** Nombre de pas pour {@code elementCount} éléments : borné entre {@link #MIN_STEPS} et {@link #MAX_STEPS}. */
	public static int length(int elementCount) {
		return Math.max(MIN_STEPS, Math.min(MAX_STEPS, elementCount));
	}

	public static List<Integer> draw(int elementCount, RandomGenerator random) {
		if (elementCount < 1) {
			throw new IllegalArgumentException("Un Simon sans élément n'a pas de suite à tirer.");
		}
		List<Integer> steps = new ArrayList<>();
		for (int step = 0; step < length(elementCount); step++) {
			steps.add(random.nextInt(elementCount));
		}
		return List.copyOf(steps);
	}
}
