package com.appv4.auth.application.service;

/**
 * Seuils des badges à compteur (BACK-19).
 *
 * <p><b>Business :</b> le seuil suivant est le précédent × 1,3, arrondi à l'entier au-dessus.
 * <p><b>Technique :</b> calcul entier, sans flottant.
 * <p><b>Métier / flow :</b> lecture des badges, pour la jauge et le niveau.
 */
public final class ChildBadgeRules {

	private ChildBadgeRules() {
	}

	public static int level(int value, int firstThreshold) {
		int level = 0;
		int threshold = firstThreshold;
		while (value >= threshold) {
			level++;
			threshold = nextAfter(threshold);
		}
		return level;
	}

	/** Seuil pas encore atteint. Au niveau 0, c'est le premier seuil. */
	public static int nextThreshold(int level, int firstThreshold) {
		int threshold = firstThreshold;
		for (int i = 0; i < level; i++) {
			threshold = nextAfter(threshold);
		}
		return threshold;
	}

	static int nextAfter(int threshold) {
		return (threshold * 13 + 9) / 10;
	}
}
