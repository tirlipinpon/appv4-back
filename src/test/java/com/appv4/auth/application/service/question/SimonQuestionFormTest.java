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
	void aColorHasNoWordAndTextStaysFree() {
		Prepared prepared = form.prepare("  ", new SimonInput(null, List.of(
				new SimonElementInput(null, " #FF0000 "),
				new SimonElementInput("chat", null),
				new SimonElementInput(null, "#FF0000")),
				List.of()));

		assertThat(prepared.prompt()).isEmpty();
		SimonDraft draft = (SimonDraft) prepared.content();
		assertThat(draft.elementType()).isEqualTo("libre");
		assertThat(draft.elements()).containsExactly(
				new SimonElementDraft(null, "#ff0000"),
				new SimonElementDraft("chat", null),
				new SimonElementDraft(null, "#ff0000"));
	}

	@Test
	void fewerThanTwoElementsIsRefused() {
		refused(new SimonInput(null, List.of(new SimonElementInput("1", null)), List.of()),
				TeacherQuestionService.SIMON_ELEMENTS_REQUIRED);
		refused(new SimonInput(null, null, List.of()), TeacherQuestionService.SIMON_ELEMENTS_REQUIRED);
	}

	@Test
	void anEmptyElementIsRefused() {
		refused(new SimonInput(null, List.of(
				new SimonElementInput("chat", null),
				new SimonElementInput("  ", null)), List.of()),
				TeacherQuestionService.SIMON_FACE);
		refused(new SimonInput(null, Arrays.asList(new SimonElementInput("chat", null), null), List.of()),
				TeacherQuestionService.SIMON_FACE);
	}

	@Test
	void twoFacesOnOneElementAreRefused() {
		refused(new SimonInput(null, List.of(
				new SimonElementInput("rouge", "#ff0000"),
				new SimonElementInput("chat", null)), List.of()),
				TeacherQuestionService.SIMON_FACE);
	}

	@Test
	void aColorNeedsACode() {
		refused(new SimonInput(null, List.of(
				new SimonElementInput(null, "blue"),
				new SimonElementInput("chat", null)), List.of()),
				TeacherQuestionService.SIMON_COLOR_REQUIRED);
	}

	@Test
	void aTooLongNameIsRefused() {
		refused(new SimonInput(null, List.of(
				new SimonElementInput("x".repeat(201), null),
				new SimonElementInput("chat", null)), List.of()),
				TeacherQuestionService.SIMON_ELEMENT_NAME_TOO_LONG);
	}

	private void refused(SimonInput input, String message) {
		assertThatThrownBy(() -> form.prepare("", input))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(message);
	}
}
