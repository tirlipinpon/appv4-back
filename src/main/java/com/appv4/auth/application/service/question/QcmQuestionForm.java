package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.CHOICE_TOO_LONG;
import static com.appv4.auth.application.service.TeacherQuestionService.CHOICES_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.CORRECT_REQUIRED;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import java.util.ArrayList;
import java.util.List;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.LegacyInput;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ChoiceInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ChoiceDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ChoicesDraft;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/** Règles de saisie de la forme qcm. */
public final class QcmQuestionForm implements QuestionFormHandler {

	private static final int CHOICE_MAX = 200;

	@Override
	public FormLine form() {
		return new FormLine("qcm", "Choix multiples", 1);
	}

	@Override
	public Prepared prepare(String prompt, ContentInput input) {
		if (!(input instanceof LegacyInput legacy)) {
			throw new IllegalArgumentException("Entrée incompatible avec qcm");
		}
		return new Prepared(QuestionValidation.requirePrompt(prompt), new ChoicesDraft(requireChoices(legacy.choices())));
	}

	private static List<ChoiceDraft> requireChoices(List<ChoiceInput> choices) {
		List<ChoiceDraft> kept = new ArrayList<>();
		if (choices != null) {
			for (ChoiceInput choice : choices) {
				if (choice == null) {
					continue;
				}
				String label = choice.label() == null ? "" : choice.label().trim();
				if (label.isEmpty()) {
					continue;
				}
				if (label.length() > CHOICE_MAX) {
					throw bad(CHOICE_TOO_LONG);
				}
				kept.add(new ChoiceDraft(label, Boolean.TRUE.equals(choice.correct())));
			}
		}
		if (kept.size() < 2) {
			throw bad(CHOICES_REQUIRED);
		}
		boolean oneCorrect = false;
		for (ChoiceDraft choice : kept) {
			if (choice.correct()) {
				oneCorrect = true;
				break;
			}
		}
		if (!oneCorrect) {
			throw bad(CORRECT_REQUIRED);
		}
		return List.copyOf(kept);
	}

}
