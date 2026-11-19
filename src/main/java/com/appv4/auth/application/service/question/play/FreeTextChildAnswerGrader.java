package com.appv4.auth.application.service.question.play;

import java.util.List;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.service.question.FreeTextMatch;

/**
 * Note une réponse libre tapée dans un seul champ (ADMIN-18).
 *
 * <p><b>Business :</b> « paris », « PARIS » et « Pâris » avec des espaces au bord
 * comptent comme « Paris ». « Lyon », « Parix » et « Pa ris » ne comptent pas.
 * Un champ vide n'est pas une partie. Ce n'est pas une case par lettre.
 * <p><b>Technique :</b> {@link FreeTextMatch} ; score 100 ou 0. Une ligne
 * {@code target_kind = texte} sans bool ni choice.
 * <p><b>Métier / flow :</b> POST attempts → {@link com.appv4.auth.application.service.ChildQuestionPlayService}.
 */
public final class FreeTextChildAnswerGrader {

	public static final String FORM = "texte";
	public static final String BAD_ANSWER = "Réponse invalide pour cette question.";
	public static final String TARGET_KIND = "texte";

	private FreeTextChildAnswerGrader() {
	}

	public static ChildAnswerGrade grade(StoredQuestion question, String attempt) {
		String expected = question.expectedAnswer();
		if (expected == null || expected.isBlank()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		if (attempt == null || attempt.isBlank()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		boolean passed = FreeTextMatch.accepted(expected, attempt);
		int score = passed ? 100 : 0;
		List<AnswerLine> lines = List.of(new AnswerLine(TARGET_KIND, question.id(), null, null, passed));
		return new ChildAnswerGrade(score, passed, lines);
	}
}
