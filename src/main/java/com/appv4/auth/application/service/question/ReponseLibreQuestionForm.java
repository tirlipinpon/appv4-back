package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.ANSWER_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.ANSWER_TOO_LONG;
import static com.appv4.auth.application.service.TeacherQuestionService.REPONSE_LIBRE;
import static com.appv4.auth.application.service.TeacherQuestionService.REPONSE_LIBRE_LABEL;
import static com.appv4.auth.application.service.TeacherQuestionService.REPONSE_LIBRE_ORDER;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.FreeTextInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort.FreeTextDraft;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/**
 * Règles de saisie de la forme lettre par lettre (SCRUM-182).
 *
 * <p><b>Business :</b> le professeur écrit l'énoncé et le mot attendu.
 * Les deux sont obligatoires. L'élève verra une case par lettre.
 * <p><b>Technique :</b> code {@code reponse_libre}, ordre 9. Le mot tient en 500 caractères.
 * <p><b>Métier / flow :</b> le registre appelle {@link #prepare} avant l'enregistrement.
 * {@link LetterByLetterMatch} jugera ce que l'élève a composé.
 */
public final class ReponseLibreQuestionForm implements QuestionFormHandler {

	private static final int ANSWER_MAX = 500;

	@Override
	public FormLine form() {
		return new FormLine(REPONSE_LIBRE, REPONSE_LIBRE_LABEL, REPONSE_LIBRE_ORDER);
	}

	@Override
	public Prepared prepare(String prompt, ContentInput input) {
		if (!(input instanceof FreeTextInput text)) {
			throw new IllegalArgumentException("Entrée incompatible avec la saisie lettre par lettre");
		}
		return new Prepared(QuestionValidation.requirePrompt(prompt), new FreeTextDraft(requireAnswer(text.expectedAnswer())));
	}

	private static String requireAnswer(String expectedAnswer) {
		String answer = expectedAnswer == null ? "" : expectedAnswer.trim();
		if (answer.isEmpty()) {
			throw bad(ANSWER_REQUIRED);
		}
		if (answer.length() > ANSWER_MAX) {
			throw bad(ANSWER_TOO_LONG);
		}
		return answer;
	}
}
