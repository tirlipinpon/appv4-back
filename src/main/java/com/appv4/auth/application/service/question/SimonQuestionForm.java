package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.SIMON;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_COLOR_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_ELEMENTS_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_ELEMENT_NAME_TOO_LONG;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_FACE;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_LABEL;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_ORDER;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.SimonElementInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.SimonInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort.SimonDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.SimonElementDraft;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/**
 * Règles de saisie d'un Simon (ADMIN-25, BACK-54).
 *
 * <p><b>Business :</b> chaque élément est une image, une couleur sans mot, ou un texte libre.
 * On peut les mélanger, et deux éléments peuvent se ressembler. La suite est tirée à la partie.
 * L'énoncé peut rester vide.
 * <p><b>Technique :</b> code {@code simon}, ordre 7. Un élément vide est refusé, pas ignoré :
 * Le code couleur est rangé en minuscules ({@code #rrggbb}).
 * <p><b>Métier / flow :</b> le registre appelle {@link #prepare} avant l'enregistrement.
 * L'élève regarde ensuite la suite, la refait, et le serveur la note.
 */
public final class SimonQuestionForm implements QuestionFormHandler {

	private static final int NAME_MAX = 200;
	private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

	@Override
	public FormLine form() {
		return new FormLine(SIMON, SIMON_LABEL, SIMON_ORDER);
	}

	@Override
	public Prepared prepare(String prompt, ContentInput input) {
		if (!(input instanceof SimonInput simon)) {
			throw new IllegalArgumentException("Entrée incompatible avec le Simon");
		}
		return new Prepared(QuestionValidation.optionalPrompt(prompt), new SimonDraft("libre", requireElements(simon.elements())));
	}

	private static List<SimonElementDraft> requireElements(List<SimonElementInput> elements) {
		if (elements == null || elements.size() < 2) {
			throw bad(SIMON_ELEMENTS_REQUIRED);
		}
		List<SimonElementDraft> drafts = new ArrayList<>();
		for (SimonElementInput element : elements) {
			if (element == null) {
				throw bad(SIMON_FACE);
			}
			byte[] image = element.imageWebp() == null || element.imageWebp().isBlank()
					? null
					: DrawnImageRules.requireWebp(element.imageWebp());
			String text = element.label() == null ? "" : element.label().trim();
			String colorRaw = element.color() == null ? "" : element.color().trim();
			boolean color = !colorRaw.isEmpty();
			int faces = (image != null ? 1 : 0) + (!text.isEmpty() ? 1 : 0) + (color ? 1 : 0);
			if (faces != 1) {
				throw bad(SIMON_FACE);
			}
			if (text.length() > NAME_MAX) {
				throw bad(SIMON_ELEMENT_NAME_TOO_LONG);
			}
			String storedColor = null;
			if (color) {
				if (!COLOR.matcher(colorRaw).matches()) {
					throw bad(SIMON_COLOR_REQUIRED);
				}
				storedColor = colorRaw.toLowerCase(Locale.ROOT);
				text = "";
			}
			drafts.add(new SimonElementDraft(text.isEmpty() ? null : text, storedColor, image));
		}
		return List.copyOf(drafts);
	}
}
