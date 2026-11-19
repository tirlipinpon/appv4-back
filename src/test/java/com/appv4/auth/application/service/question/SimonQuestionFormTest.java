package com.appv4.auth.application.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.SimonElementInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.SimonInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort.SimonDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.SimonElementDraft;
import com.appv4.auth.application.service.TeacherQuestionService;
import com.appv4.auth.application.service.question.QuestionFormHandler.Prepared;

/** ADMIN-25 / BACK-54 : ce que le professeur peut enregistrer comme Simon. */
class SimonQuestionFormTest {

	private final SimonQuestionForm form = new SimonQuestionForm();

	@Test
	void formIsSimonAtRankSeven() {
		assertThat(form.form().code()).isEqualTo("simon");
		assertThat(form.form().label()).isEqualTo("Simon");
		assertThat(form.form().sortOrder()).isEqualTo(7);
	}

	@Test
	void colorsKeepNamesCodesInLowerCaseAndTheSequence() {
		Prepared prepared = form.prepare("  ", new SimonInput(" couleurs ", List.of(
				new SimonElementInput(" rouge ", "#FF0000"),
				new SimonElementInput("bleu", " #0000ff "),
				new SimonElementInput("vert", "#00FF00")),
				List.of(0, 1, 2, 0)));

		assertThat(prepared.prompt()).isEmpty();
		SimonDraft draft = (SimonDraft) prepared.content();
		assertThat(draft.elementType()).isEqualTo("couleurs");
		assertThat(draft.elements()).containsExactly(
				new SimonElementDraft("rouge", "#ff0000"),
				new SimonElementDraft("bleu", "#0000ff"),
				new SimonElementDraft("vert", "#00ff00"));
	}

	@Test
	void numbersLettersAndShapesHaveNoColor() {
		SimonDraft numbers = draft("nombres", List.of(element("7", null), element("-2", ""), element("3,5", null)));
		SimonDraft letters = draft("lettres", List.of(element("A", null), element("é", null)));
		SimonDraft shapes = draft("formes", List.of(element("cercle", null), element("carré", null)));

		assertThat(numbers.elements()).extracting(SimonElementDraft::label).containsExactly("7", "-2", "3,5");
		assertThat(numbers.elements()).extracting(SimonElementDraft::color).containsOnlyNulls();
		assertThat(letters.elements()).extracting(SimonElementDraft::label).containsExactly("A", "é");
		assertThat(shapes.elements()).extracting(SimonElementDraft::label).containsExactly("cercle", "carré");
	}

	@Test
	void unknownOrMissingTypeIsRefused() {
		refused(new SimonInput("symboles", twoNumbers(), List.of(0, 1)), TeacherQuestionService.SIMON_TYPE_INVALID);
		refused(new SimonInput(null, twoNumbers(), List.of(0, 1)), TeacherQuestionService.SIMON_TYPE_INVALID);
		refused(new SimonInput("Nombres", twoNumbers(), List.of(0, 1)), TeacherQuestionService.SIMON_TYPE_INVALID);
	}

	@Test
	void fewerThanTwoElementsIsRefused() {
		refused(new SimonInput("nombres", List.of(element("1", null)), List.of(0, 0)),
				TeacherQuestionService.SIMON_ELEMENTS_REQUIRED);
		refused(new SimonInput("nombres", null, List.of(0, 0)), TeacherQuestionService.SIMON_ELEMENTS_REQUIRED);
	}

	@Test
	void aBlankOrMissingElementIsRefusedInsteadOfShiftingTheSequence() {
		refused(new SimonInput("nombres", List.of(element("1", null), element("  ", null), element("3", null)),
				List.of(0, 2)), TeacherQuestionService.SIMON_ELEMENT_NAME_REQUIRED);
		refused(new SimonInput("nombres", Arrays.asList(element("1", null), null), List.of(0, 1)),
				TeacherQuestionService.SIMON_ELEMENT_NAME_REQUIRED);
	}

	@Test
	void aTooLongNameIsRefused() {
		refused(new SimonInput("formes", List.of(element("x".repeat(201), null), element("cercle", null)),
				List.of(0, 1)), TeacherQuestionService.SIMON_ELEMENT_NAME_TOO_LONG);
	}

	@Test
	void anElementOutsideTheChosenTypeIsRefused() {
		refused(new SimonInput("nombres", List.of(element("1", null), element("rouge", null)), List.of(0, 1)),
				TeacherQuestionService.SIMON_NUMBER_REQUIRED);
		refused(new SimonInput("nombres", List.of(element("1", null), element("2", "#ff0000")), List.of(0, 1)),
				TeacherQuestionService.SIMON_COLOR_FORBIDDEN);
		refused(new SimonInput("lettres", List.of(element("A", null), element("AB", null)), List.of(0, 1)),
				TeacherQuestionService.SIMON_LETTER_REQUIRED);
		refused(new SimonInput("lettres", List.of(element("A", null), element("4", null)), List.of(0, 1)),
				TeacherQuestionService.SIMON_LETTER_REQUIRED);
		refused(new SimonInput("formes", List.of(element("cercle", null), element("carré", "#00ff00")), List.of(0, 1)),
				TeacherQuestionService.SIMON_COLOR_FORBIDDEN);
	}

	@Test
	void aColorWithoutAValidCodeIsRefused() {
		refused(new SimonInput("couleurs", List.of(element("rouge", "#ff0000"), element("bleu", null)), List.of(0, 1)),
				TeacherQuestionService.SIMON_COLOR_REQUIRED);
		refused(new SimonInput("couleurs", List.of(element("rouge", "#ff0000"), element("bleu", "blue")), List.of(0, 1)),
				TeacherQuestionService.SIMON_COLOR_REQUIRED);
		refused(new SimonInput("couleurs", List.of(element("rouge", "#ff0000"), element("bleu", "#00f")), List.of(0, 1)),
				TeacherQuestionService.SIMON_COLOR_REQUIRED);
	}

	@Test
	void twoElementsTheChildCannotTellApartAreRefused() {
		refused(new SimonInput("lettres", List.of(element("A", null), element("a", null)), List.of(0, 1)),
				TeacherQuestionService.SIMON_ELEMENT_DUPLICATE);
		refused(new SimonInput("couleurs", List.of(element("rouge", "#FF0000"), element("carmin", "#ff0000")),
				List.of(0, 1)), TeacherQuestionService.SIMON_COLOR_DUPLICATE);
	}

	private SimonDraft draft(String type, List<SimonElementInput> elements) {
		return draft(type, elements, List.of(0, 1));
	}

	private SimonDraft draft(String type, List<SimonElementInput> elements, List<Integer> sequence) {
		return (SimonDraft) form.prepare("", new SimonInput(type, elements, sequence)).content();
	}

	private void refused(SimonInput input, String message) {
		assertThatThrownBy(() -> form.prepare("", input))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(message);
	}

	private static List<SimonElementInput> twoNumbers() {
		return List.of(element("1", null), element("2", null));
	}

	private static SimonElementInput element(String label, String color) {
		return new SimonElementInput(label, color);
	}
}
