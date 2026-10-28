package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.PAIRS_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.PAIR_TOO_LONG;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import java.util.ArrayList;
import java.util.List;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.LegacyInput;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.PairInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PairsDraft;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/** Règles de saisie de la forme memory. */
public final class MemoryQuestionForm implements QuestionFormHandler {

	private static final int CHOICE_MAX = 200;

	@Override
	public FormLine form() {
		return new FormLine("memory", "Memory", 2);
	}

	@Override
	public Prepared prepare(String prompt, ContentInput input) {
		if (!(input instanceof LegacyInput legacy)) {
			throw new IllegalArgumentException("Entrée incompatible avec memory");
		}
		return new Prepared(QuestionValidation.optionalPrompt(prompt), new PairsDraft(requirePairs(legacy.pairs())));
	}

	private static List<TeacherQuestionPort.PairDraft> requirePairs(List<PairInput> pairs) {
		List<TeacherQuestionPort.PairDraft> kept = new ArrayList<>();
		if (pairs != null) {
			for (PairInput pair : pairs) {
				if (pair == null) {
					continue;
				}
				String questionSide = pair.questionSide() == null ? "" : pair.questionSide().trim();
				String answerSide = pair.answerSide() == null ? "" : pair.answerSide().trim();
				if (questionSide.isEmpty() || answerSide.isEmpty()) {
					continue;
				}
				if (questionSide.length() > CHOICE_MAX || answerSide.length() > CHOICE_MAX) {
					throw bad(PAIR_TOO_LONG);
				}
				kept.add(new TeacherQuestionPort.PairDraft(questionSide, answerSide));
			}
		}
		if (kept.size() < 2) {
			throw bad(PAIRS_REQUIRED);
		}
		return List.copyOf(kept);
	}

}
