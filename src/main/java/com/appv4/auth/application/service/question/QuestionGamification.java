package com.appv4.auth.application.service.question;

import java.util.ArrayList;
import java.util.List;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;

/**
 * Barème des questions (BACK-45).
 *
 * <p><b>Business :</b> une question neuve part avec 1 essai, la difficulté Facile
 * et la gamification allumée. Le professeur n'a rien à régler s'il ne veut pas.
 * Couper la gamification laisse la question jouable, dans les statistiques, avec ses points.
 * <p><b>Technique :</b> codes stables, libellés et points du contrat. Le front affiche ça, il ne l'invente pas.
 * <p><b>Métier / flow :</b> create/update teacher question → colonnes de la question → GET.
 */
public final class QuestionGamification {

	public static final int MIN_ATTEMPTS = 1;
	public static final int MAX_ATTEMPTS = 5;
	public static final String EASY = "facile";

	public static final String UNKNOWN_DIFFICULTY = "Difficulté inconnue.";
	public static final String ATTEMPTS_RANGE = "Le nombre d'essais doit être entre 1 et 5.";
	public static final String POINTS_LENGTH = "Il faut un nombre de points par essai, autant que d'essais.";
	public static final String POINTS_NEGATIVE = "Les points d'un essai ne peuvent pas être négatifs.";

	public record Difficulty(String code, String label, List<Integer> pointsByAttempt) {
	}

	public record Resolved(
			int maxAttempts,
			String difficulty,
			String difficultyLabel,
			boolean customizePoints,
			boolean gamificationDisabled,
			List<Integer> points) {
	}

	private static final List<Difficulty> SCALE = List.of(
			new Difficulty(EASY, "Facile", List.of(10, 5, 2, 1, 1)),
			new Difficulty("modere", "Modéré", List.of(20, 10, 5, 2, 1)),
			new Difficulty("difficile", "Difficile", List.of(30, 15, 8, 4, 2)));

	private QuestionGamification() {
	}

	public static List<Difficulty> scale() {
		return SCALE;
	}

	public static Resolved defaults() {
		return resolve(null, null, null, null, null);
	}

	/**
	 * Un champ absent reprend le défaut. Personnaliser exige une valeur par essai.
	 * Sinon les points viennent du barème de la difficulté, tronqué au nombre d'essais.
	 */
	public static Resolved resolve(
			Integer maxAttempts,
			String difficulty,
			Boolean customizePoints,
			Boolean gamificationDisabled,
			List<Integer> customPoints) {
		int attempts = maxAttempts == null ? MIN_ATTEMPTS : maxAttempts;
		if (attempts < MIN_ATTEMPTS || attempts > MAX_ATTEMPTS) {
			throw bad(ATTEMPTS_RANGE);
		}
		String code = difficulty == null || difficulty.isBlank() ? EASY : difficulty.trim();
		Difficulty level = find(code);
		if (level == null) {
			throw bad(UNKNOWN_DIFFICULTY);
		}
		boolean custom = Boolean.TRUE.equals(customizePoints);
		List<Integer> points = custom
				? requireCustom(attempts, customPoints)
				: level.pointsByAttempt().subList(0, attempts);
		return new Resolved(
				attempts,
				level.code(),
				level.label(),
				custom,
				Boolean.TRUE.equals(gamificationDisabled),
				List.copyOf(points));
	}

	private static Difficulty find(String code) {
		for (Difficulty level : SCALE) {
			if (level.code().equals(code)) {
				return level;
			}
		}
		return null;
	}

	private static List<Integer> requireCustom(int attempts, List<Integer> customPoints) {
		if (customPoints == null || customPoints.size() != attempts) {
			throw bad(POINTS_LENGTH);
		}
		List<Integer> points = new ArrayList<>();
		for (Integer point : customPoints) {
			if (point == null || point < 0) {
				throw bad(POINTS_NEGATIVE);
			}
			points.add(point);
		}
		return points;
	}

	private static AuthDomainException bad(String message) {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, message);
	}
}
