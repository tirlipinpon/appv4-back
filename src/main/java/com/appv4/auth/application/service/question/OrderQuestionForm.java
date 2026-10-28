package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.ITEMS_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.ITEM_TOO_LONG;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import java.util.ArrayList;
import java.util.List;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.LegacyInput;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ItemInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ItemsDraft;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/** Règles de saisie de la forme order. */
public final class OrderQuestionForm implements QuestionFormHandler {

	private static final int CHOICE_MAX = 200;

	@Override
	public FormLine form() {
		return new FormLine("order", "Ordre", 3);
	}

	@Override
	public Prepared prepare(String prompt, ContentInput input) {
		if (!(input instanceof LegacyInput legacy)) {
			throw new IllegalArgumentException("Entrée incompatible avec order");
		}
		return new Prepared(QuestionValidation.optionalPrompt(prompt), new ItemsDraft(requireItems(legacy.items())));
	}

	private static List<TeacherQuestionPort.ItemDraft> requireItems(List<ItemInput> items) {
		List<TeacherQuestionPort.ItemDraft> kept = new ArrayList<>();
		if (items != null) {
			for (ItemInput item : items) {
				if (item == null) {
					continue;
				}
				String label = item.label() == null ? "" : item.label().trim();
				if (label.isEmpty()) {
					continue;
				}
				if (label.length() > CHOICE_MAX) {
					throw bad(ITEM_TOO_LONG);
				}
				kept.add(new TeacherQuestionPort.ItemDraft(label));
			}
		}
		if (kept.size() < 2) {
			throw bad(ITEMS_REQUIRED);
		}
		return List.copyOf(kept);
	}

}
