package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.AXIS_LABEL_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.AXIS_LABEL_TOO_LONG;
import static com.appv4.auth.application.service.TeacherQuestionService.ITEMS_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.ITEM_TOO_LONG;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import java.util.ArrayList;
import java.util.List;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ItemInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.LegacyInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.OrderInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ItemsDraft;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/**
 * Règles de saisie de la forme order (SCRUM-176, BACK-46).
 *
 * <p><b>Business :</b> au moins deux étapes ; frise optionnelle avec un libellé d'axe par jalon.
 * <p><b>Technique :</b> accepte {@link OrderInput} ou {@link LegacyInput} (sans frise).
 * <p><b>Métier / flow :</b> prépare un {@link ItemsDraft} pour le store Ordre.
 */
public final class OrderQuestionForm implements QuestionFormHandler {

	private static final int CHOICE_MAX = 200;
	private static final int AXIS_MAX = 80;

	@Override
	public FormLine form() {
		return new FormLine("order", "Ordre", 3);
	}

	@Override
	public Prepared prepare(String prompt, ContentInput input) {
		boolean timelineEnabled = false;
		List<ItemInput> items;
		if (input instanceof OrderInput order) {
			timelineEnabled = Boolean.TRUE.equals(order.timelineEnabled());
			items = order.items();
		} else if (input instanceof LegacyInput legacy) {
			items = legacy.items();
		} else {
			throw new IllegalArgumentException("Entrée incompatible avec order");
		}
		return new Prepared(
				QuestionValidation.optionalPrompt(prompt),
				new ItemsDraft(requireItems(items, timelineEnabled), timelineEnabled));
	}

	private static List<TeacherQuestionPort.ItemDraft> requireItems(List<ItemInput> items, boolean timelineEnabled) {
		List<TeacherQuestionPort.ItemDraft> kept = new ArrayList<>();
		if (items != null) {
			for (ItemInput item : items) {
				if (item == null) {
					continue;
				}
				PieceFaces.Face face = PieceFaces.optional(item.label(), item.imageWebp(), CHOICE_MAX, ITEM_TOO_LONG);
				if (face == null) {
					continue;
				}
				String axis = item.axisLabel() == null ? "" : item.axisLabel().trim();
				if (timelineEnabled) {
					if (axis.isEmpty()) {
						throw bad(AXIS_LABEL_REQUIRED);
					}
					if (axis.length() > AXIS_MAX) {
						throw bad(AXIS_LABEL_TOO_LONG);
					}
					kept.add(new TeacherQuestionPort.ItemDraft(face.label(), axis, face.imageWebp()));
				} else {
					kept.add(new TeacherQuestionPort.ItemDraft(face.label(), null, face.imageWebp()));
				}
			}
		}
		if (kept.size() < 2) {
			throw bad(ITEMS_REQUIRED);
		}
		return List.copyOf(kept);
	}

}
