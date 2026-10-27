package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ChoiceInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ChoiceDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoice;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.QuestionLine;

@ExtendWith(MockitoExtension.class)
class TeacherQuestionServiceTest {

	private static final long TEACHER_ID = 9L;
	private static final long SUBJECT_ID = 4L;

	@Mock
	private TeacherSubjectPort subjects;

	@Mock
	private TeacherQuestionPort questions;

	private TeacherQuestionService service;

	@BeforeEach
	void setUp() {
		service = new TeacherQuestionService(subjects, questions);
	}

	@Test
	void offeredFormsAreOnlyMultipleChoice() {
		assertThat(service.listForms(TEACHER_ID, "teacher").forms())
				.singleElement()
				.satisfies(form -> {
					assertThat(form.code()).isEqualTo("qcm");
					assertThat(form.label()).isEqualTo("Choix multiples");
					assertThat(form.sortOrder()).isEqualTo(1);
				});
	}

	@Test
	void parentCannotListForms() {
		assertThatThrownBy(() -> service.listForms(TEACHER_ID, "parent"))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.FORBIDDEN);
	}

	@Test
	void createKeepsTheSubjectAndDropsABlankChoice() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID))
				.thenReturn(Optional.of(owned()));
		when(questions.insert(eq(SUBJECT_ID), eq("qcm"), eq("Combien font 2 + 2 ?"), anyList()))
				.thenReturn(saved());

		QuestionLine line = service.create(
				TEACHER_ID,
				"teacher",
				SUBJECT_ID,
				" qcm ",
				"  Combien font 2 + 2 ?  ",
				List.of(
						new ChoiceInput(" 3 ", false),
						new ChoiceInput("   ", true),
						new ChoiceInput("4", true)));

		assertThat(line.prompt()).isEqualTo("Combien font 2 + 2 ?");
		assertThat(line.formCode()).isEqualTo("qcm");
		assertThat(line.formLabel()).isEqualTo("Choix multiples");
		verify(questions).ensureForm(SUBJECT_ID, "qcm", "Choix multiples", 1);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<ChoiceDraft>> captor = ArgumentCaptor.forClass(List.class);
		verify(questions).insert(eq(SUBJECT_ID), eq("qcm"), eq("Combien font 2 + 2 ?"), captor.capture());
		assertThat(captor.getValue()).containsExactly(
				new ChoiceDraft("3", false),
				new ChoiceDraft("4", true));
	}

	@Test
	void emptyPromptIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "qcm", "  ",
				List.of(new ChoiceInput("3", false), new ChoiceInput("4", true))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.PROMPT_REQUIRED);

		verify(questions, never()).insert(eq(SUBJECT_ID), eq("qcm"), eq("  "), anyList());
	}

	@Test
	void oneChoiceIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "qcm", "Combien font 2 + 2 ?",
				List.of(new ChoiceInput("4", true), new ChoiceInput(" ", false))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.CHOICES_REQUIRED);
	}

	@Test
	void missingCorrectAnswerIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "qcm", "Combien font 2 + 2 ?",
				List.of(new ChoiceInput("3", false), new ChoiceInput("4", null))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.CORRECT_REQUIRED);
	}

	@Test
	void unknownFormIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "memory", "Paires",
				List.of(new ChoiceInput("2", false), new ChoiceInput("4", true))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.FORM_UNKNOWN);

		verify(questions, never()).ensureForm(SUBJECT_ID, "memory", "Memory", 2);
	}

	@Test
	void updateRewritesTheStatementAndKeepsTheForm() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.findInSubject(SUBJECT_ID, 7L)).thenReturn(Optional.of(saved()));
		when(questions.replace(eq(7L), eq("Combien font 3 + 3 ?"), anyList()))
				.thenReturn(new StoredQuestion(
						7L,
						SUBJECT_ID,
						"qcm",
						"Combien font 3 + 3 ?",
						List.of(new StoredChoice(3L, "5", false), new StoredChoice(4L, "6", true))));

		QuestionLine line = service.update(
				TEACHER_ID,
				"teacher",
				SUBJECT_ID,
				7L,
				"qcm",
				"  Combien font 3 + 3 ? ",
				List.of(new ChoiceInput("5", false), new ChoiceInput(" ", false), new ChoiceInput("6", true)));

		assertThat(line.id()).isEqualTo(7L);
		assertThat(line.prompt()).isEqualTo("Combien font 3 + 3 ?");
		assertThat(line.formCode()).isEqualTo("qcm");
		verify(questions, never()).insert(eq(SUBJECT_ID), eq("qcm"), eq("Combien font 3 + 3 ?"), anyList());
		verify(questions, never()).ensureForm(SUBJECT_ID, "qcm", "Choix multiples", 1);
	}

	@Test
	void missingQuestionIsNotFound() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.findInSubject(SUBJECT_ID, 8L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(
				TEACHER_ID, "teacher", SUBJECT_ID, 8L, "qcm", "Combien font 2 + 2 ?",
				List.of(new ChoiceInput("3", false), new ChoiceInput("4", true))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.QUESTION_MISSING);

		verify(questions, never()).replace(eq(8L), eq("Combien font 2 + 2 ?"), anyList());
	}

	@Test
	void changingTheFormOfAnExistingQuestionIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.findInSubject(SUBJECT_ID, 7L)).thenReturn(Optional.of(new StoredQuestion(
				7L, SUBJECT_ID, "vrai_faux", "C'est vrai ?", List.of())));

		assertThatThrownBy(() -> service.update(
				TEACHER_ID, "teacher", SUBJECT_ID, 7L, "qcm", "Combien font 2 + 2 ?",
				List.of(new ChoiceInput("3", false), new ChoiceInput("4", true))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.FORM_LOCKED);

		verify(questions, never()).replace(eq(7L), eq("Combien font 2 + 2 ?"), anyList());
	}

	@Test
	void anotherTeacherDoesNotSeeTheQuestion() {
		when(subjects.findOwned(10L, SUBJECT_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.list(10L, "teacher", SUBJECT_ID))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.NOT_FOUND);

		verify(questions, never()).findBySubject(SUBJECT_ID);
	}

	private static StoredSubject owned() {
		return new StoredSubject(SUBJECT_ID, TEACHER_ID, 1L, "CE1", "Mathématiques", true);
	}

	private static StoredQuestion saved() {
		return new StoredQuestion(
				7L,
				SUBJECT_ID,
				"qcm",
				"Combien font 2 + 2 ?",
				List.of(new StoredChoice(1L, "3", false), new StoredChoice(2L, "4", true)));
	}
}
