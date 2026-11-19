package com.appv4.auth.application.service.question.play;

import java.util.ArrayList;
import java.util.List;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredSimonElement;

/**
 * Note un Simon enfant : la suite refaite doit être exactement celle du professeur.
 *
 * <p><b>Business :</b> l'élève regarde la suite s'allumer, puis la refait. Il n'y a pas de
 * bouton Valider : l'écran envoie la reprise quand tous les pas sont joués. Même suite :
 * 100 et réussi. Un seul pas différent (autre élément, autre ordre) : 0, pas de score partiel.
 * <p><b>Technique :</b> compare {@code simonSequence} (positions 0-based dans les éléments) à la
 * suite enregistrée. Une longueur différente ou une position inconnue est une réponse invalide
 * (400). Une ligne par pas : {@code target_kind = simon_step}, {@code target_id} = élément attendu,
 * {@code answer_choice_id} = élément touché, pour rester dans la contrainte des tentatives.
 * <p><b>Métier / flow :</b> POST attempts → {@link com.appv4.auth.application.service.ChildQuestionPlayService}.
 */
public final class SimonChildAnswerGrader {

	public static final String FORM = "simon";
	public static final String BAD_ANSWER = "Réponse invalide pour cette question.";
	public static final String TARGET_KIND = "simon_step";

	private SimonChildAnswerGrader() {
	}

	public static ChildAnswerGrade grade(StoredQuestion question, List<Integer> replayed) {
		List<StoredSimonElement> elements = question.simonElements();
		List<Integer> expected = question.simonSequence();
		if (elements.isEmpty() || expected.isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		List<Integer> submitted = replayed == null ? List.of() : replayed;
		if (submitted.size() != expected.size()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		boolean exact = true;
		List<AnswerLine> lines = new ArrayList<>();
		for (int step = 0; step < expected.size(); step++) {
			Integer chosen = submitted.get(step);
			if (chosen == null || chosen < 0 || chosen >= elements.size()) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
			}
			int wanted = expected.get(step);
			boolean correct = chosen == wanted;
			exact = exact && correct;
			lines.add(new AnswerLine(
					TARGET_KIND, elements.get(wanted).id(), null, elements.get(chosen).id(), correct));
		}
		return new ChildAnswerGrade(exact ? 100 : 0, exact, lines);
	}
}
