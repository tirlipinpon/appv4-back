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
 * <p><b>Business :</b> le mot assemblé doit coller au mot du professeur
 * (casse ignorée, accents gardés). Réussi seulement à 100 %.
 * <p><b>Technique :</b> réutilise {@link LetterByLetterMatch} ; une ligne
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
		boolean passed = LetterByLetterMatch.accepted(expected, attempt);
		int score = passed ? 100 : 0;
		List<AnswerLine> lines = List.of(new AnswerLine("letter", question.id(), null, null, passed));
		return new ChildAnswerGrade(score, passed, lines);
	}
}
