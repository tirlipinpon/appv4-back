package com.appv4.auth.application.service.question.play;

import java.util.ArrayList;
import java.util.List;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredSimonElement;

/**
 * Note un Simon enfant : la reprise doit être exactement la suite tirée pour lui (BACK-54, BACK-55).
 *
 * <p><b>Business :</b> à l'ouverture, le serveur tire une suite au hasard parmi les éléments du
 * professeur. L'écran montre d'abord 1 pas, puis 2 depuis le début, puis 3, jusqu'à la suite
 * tirée. Même suite jusqu'au bout : 100 et réussi. Une erreur au milieu : score = pas justes
 * depuis le début / longueur de la suite, pas réussi.
 * <p><b>Technique :</b> compare {@code simonSequence} (positions 0-based dans les éléments) à la
 * suite gardée pour cet enfant. Pas de suite gardée, ou une suite qui ne tombe plus dans les
 * éléments (question corrigée entre-temps) : {@link #SEQUENCE_MISSING} (400). Une reprise de
 * longueur différente ou avec une position inconnue : {@link #BAD_ANSWER} (400). Une ligne par pas :
 * {@code target_kind = simon_step}, {@code target_id} = élément attendu, {@code answer_choice_id} =
 * élément touché, pour rester dans la contrainte des tentatives.
 * <p><b>Métier / flow :</b> POST attempts → {@link com.appv4.auth.application.service.ChildQuestionPlayService}
 * relit la suite gardée par {@code ChildSimonChallengePort} → ce correcteur.
 */
public final class SimonChildAnswerGrader {

	public static final String FORM = "simon";
	public static final String BAD_ANSWER = "Réponse invalide pour cette question.";
	public static final String SEQUENCE_MISSING = "Rouvre la question pour voir une nouvelle suite.";
	public static final String TARGET_KIND = "simon_step";

	private SimonChildAnswerGrader() {
	}

	/**
	 * @param issued suite tirée à la dernière ouverture de l'enfant, vide s'il n'y en a pas
	 * @param replayed positions touchées par l'enfant, dans l'ordre
	 */
	public static ChildAnswerGrade grade(StoredQuestion question, List<Integer> issued, List<Integer> replayed) {
		List<StoredSimonElement> elements = question.simonElements();
		List<Integer> expected = issued == null ? List.of() : issued;
		if (expected.isEmpty() || !allWithin(expected, elements.size())) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, SEQUENCE_MISSING);
		}
		List<Integer> submitted = replayed == null ? List.of() : replayed;
		if (submitted.isEmpty() || submitted.size() > expected.size() || !allWithin(submitted, elements.size())) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		int matched = 0;
		while (matched < submitted.size() && submitted.get(matched).intValue() == expected.get(matched)) {
			matched++;
		}
		boolean exact = matched == expected.size();
		List<AnswerLine> lines = new ArrayList<>();
		for (int step = 0; step < submitted.size(); step++) {
			int wanted = expected.get(step);
			int chosen = submitted.get(step);
			lines.add(new AnswerLine(
					TARGET_KIND, elements.get(wanted).id(), null, elements.get(chosen).id(), step < matched));
		}
		int score = (int) Math.round(100.0 * matched / expected.size());
		return new ChildAnswerGrade(score, exact, lines);
	}

	private static boolean allWithin(List<Integer> positions, int elementCount) {
		return positions.stream().allMatch(index -> index != null && index >= 0 && index < elementCount);
	}
}
