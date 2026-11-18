package com.appv4.auth.application.service.question.play;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoice;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

/**
 * Note un QCM enfant : les bons choix exacts = 100 %, sinon 0 %.
 *
 * <p><b>Business :</b> cocher trop ou trop peu n'est pas une réussite.
 * <p><b>Technique :</b> compare l'ensemble sélectionné à l'ensemble correct.
 */
public final class QcmChildAnswerGrader {

	public static final String FORM = "qcm";
	public static final String BAD_ANSWER = "Réponse invalide pour cette question.";

	private QcmChildAnswerGrader() {
	}

	public static ChildAnswerGrade grade(StoredQuestion question, List<Long> selectedChoiceIds) {
		List<StoredChoice> choices = question.choices();
		if (choices.isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Set<Long> known = choices.stream().map(StoredChoice::id).collect(Collectors.toSet());
		Set<Long> selected = new HashSet<>(selectedChoiceIds == null ? List.of() : selectedChoiceIds);
		if (!known.containsAll(selected)) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Set<Long> correctIds = choices.stream()
				.filter(StoredChoice::correct)
				.map(StoredChoice::id)
				.collect(Collectors.toSet());
		boolean passed = selected.equals(correctIds);
		int score = passed ? 100 : 0;
		List<AnswerLine> lines = new ArrayList<>();
		for (Long choiceId : selected) {
			boolean ok = correctIds.contains(choiceId);
			lines.add(new AnswerLine("choice", choiceId, null, choiceId, ok));
		}
		return new ChildAnswerGrade(score, passed, lines);
	}
}
