package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.SIMON;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_COLORS;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_COLOR_DUPLICATE;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_COLOR_FORBIDDEN;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_COLOR_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_ELEMENTS_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_ELEMENT_DUPLICATE;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_ELEMENT_NAME_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_ELEMENT_NAME_TOO_LONG;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_LABEL;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_LETTERS;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_LETTER_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_NUMBERS;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_NUMBER_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_ORDER;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_SHAPES;
import static com.appv4.auth.application.service.TeacherQuestionService.SIMON_TYPE_INVALID;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
 * <p><b>Business :</b> le professeur choisit un genre (couleurs, nombres, lettres ou formes),
 * pose au moins deux éléments de ce genre. La suite n'est pas choisie ici : à la partie,
 * le serveur la tire au hasard et la rallonge d'un pas à chaque réussite (1, puis 2 depuis
 * le début, puis 3…). Un élément hors du genre est refusé : un nombre doit être un nombre, une lettre une
 * seule lettre, une couleur porte son code ; les autres genres n'ont pas de code couleur.
 * Deux éléments de même nom, ou deux couleurs de même code, sont refusés : l'élève ne pourrait
 * pas les distinguer. L'énoncé peut rester vide.
 * <p><b>Technique :</b> code {@code simon}, ordre 7. Un élément vide est refusé, pas ignoré :
 * Le code couleur est rangé en minuscules ({@code #rrggbb}).
 * <p><b>Métier / flow :</b> le registre appelle {@link #prepare} avant l'enregistrement.
 * L'élève regarde ensuite la suite, la refait, et le serveur la note.
 */
public final class SimonQuestionForm implements QuestionFormHandler {

	private static final int NAME_MAX = 200;
	private static final Pattern NUMBER = Pattern.compile("-?\\d+([.,]\\d+)?");
	private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");
	private static final Set<String> TYPES = Set.of(SIMON_COLORS, SIMON_NUMBERS, SIMON_LETTERS, SIMON_SHAPES);

	@Override
	public FormLine form() {
		return new FormLine(SIMON, SIMON_LABEL, SIMON_ORDER);
	}

	@Override
	public Prepared prepare(String prompt, ContentInput input) {
		if (!(input instanceof SimonInput simon)) {
			throw new IllegalArgumentException("Entrée incompatible avec le Simon");
		}
		String type = requireType(simon.elementType());
		List<SimonElementDraft> elements = requireElements(type, simon.elements());
		return new Prepared(QuestionValidation.optionalPrompt(prompt), new SimonDraft(type, elements));
	}

	private static String requireType(String raw) {
		String type = raw == null ? "" : raw.trim();
		if (!TYPES.contains(type)) {
			throw bad(SIMON_TYPE_INVALID);
		}
		return type;
	}

	private static List<SimonElementDraft> requireElements(String type, List<SimonElementInput> elements) {
		if (elements == null || elements.size() < 2) {
			throw bad(SIMON_ELEMENTS_REQUIRED);
		}
		List<SimonElementDraft> drafts = new ArrayList<>();
		Set<String> names = new HashSet<>();
		Set<String> colors = new HashSet<>();
		for (SimonElementInput element : elements) {
			if (element == null) {
				throw bad(SIMON_ELEMENT_NAME_REQUIRED);
			}
			String name = requireName(type, element.label());
			String color = requireColor(type, element.color());
			if (!names.add(name.toLowerCase(Locale.ROOT))) {
				throw bad(SIMON_ELEMENT_DUPLICATE);
			}
			if (color != null && !colors.add(color)) {
				throw bad(SIMON_COLOR_DUPLICATE);
			}
			drafts.add(new SimonElementDraft(name, color));
		}
		return List.copyOf(drafts);
	}

	/** Nom non vide, puis du genre choisi : nombre pour {@code nombres}, une lettre pour {@code lettres}. */
	private static String requireName(String type, String label) {
		String name = label == null ? "" : label.trim();
		if (name.isEmpty()) {
			throw bad(SIMON_ELEMENT_NAME_REQUIRED);
		}
		if (name.length() > NAME_MAX) {
			throw bad(SIMON_ELEMENT_NAME_TOO_LONG);
		}
		if (SIMON_NUMBERS.equals(type) && !NUMBER.matcher(name).matches()) {
			throw bad(SIMON_NUMBER_REQUIRED);
		}
		if (SIMON_LETTERS.equals(type) && !isOneLetter(name)) {
			throw bad(SIMON_LETTER_REQUIRED);
		}
		return name;
	}

	private static boolean isOneLetter(String name) {
		return name.codePointCount(0, name.length()) == 1 && Character.isLetter(name.codePointAt(0));
	}

	/** Couleurs : code {@code #rrggbb} obligatoire. Autres genres : aucun code. */
	private static String requireColor(String type, String raw) {
		String color = raw == null ? "" : raw.trim();
		if (!SIMON_COLORS.equals(type)) {
			if (!color.isEmpty()) {
				throw bad(SIMON_COLOR_FORBIDDEN);
			}
			return null;
		}
		if (!COLOR.matcher(color).matches()) {
			throw bad(SIMON_COLOR_REQUIRED);
		}
		return color.toLowerCase(Locale.ROOT);
	}

}
