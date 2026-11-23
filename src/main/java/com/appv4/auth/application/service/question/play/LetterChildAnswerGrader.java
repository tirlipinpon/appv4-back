package com.appv4.auth.application.service.question.play;

import java.util.List;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.service.question.LetterByLetterMatch;

/**
 * Note une saisie lettre par lettre.
 *
 * <p><b>Business :</b> chaque lettre juste compte dans le score (les espaces
 * ne comptent pas). Réussi seulement quand toutes les lettres sont justes.
 * <p><b>Technique :</b> {@link LetterByLetterMatch#scorePercent} ; une ligne
 * {@code target_kind = letter} sans bool ni choice.
 * <p><b>Métier / flow :</b> POST attempts → {@link com.appv4.auth.application.service.ChildQuestionPlayService}.
 */
public final class LetterChildAnswerGrader {

	public static final String FORM = "reponse_libre";
	public static final String BAD_ANSWER = "Réponse invalide pour cette question.";

	private LetterChildAnswerGrader() {
	}

	public static ChildAnswerGrade grade(StoredQuestion question, String attempt) {
		String expected = question.expectedAnswer();
		if (expected == null || expected.isBlank()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		if (attempt == null || attempt.isBlank()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		if (attempt.length() != expected.length()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		LetterByLetterMatch.Score score = LetterByLetterMatch.score(expected, attempt);
		boolean passed = score.passed();
		List<AnswerLine> lines = List.of(new AnswerLine("letter", question.id(), null, null, passed));
		return new ChildAnswerGrade(score.percent(), passed, lines);
	}
}
