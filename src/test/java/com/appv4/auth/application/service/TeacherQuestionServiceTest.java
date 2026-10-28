package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.appv4.auth.application.service.question.QuestionFormHandler;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ChoiceInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ItemInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.PairInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.StatementInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.StatementsInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ChoiceDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PairDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoice;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.service.question.QuestionFormRegistry;
import com.appv4.auth.application.service.question.QcmQuestionForm;
import com.appv4.auth.application.service.question.MemoryQuestionForm;
import com.appv4.auth.application.service.question.OrderQuestionForm;
import com.appv4.auth.application.service.question.TrueFalseQuestionForm;
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

	@Test
	void independentFormCarriesItsOwnStructureThroughTheCommonService() {
		var handler = new QuestionFormHandler() {
			@Override
			public FormLine form() {
				return new FormLine("image_test", "Image test", 8);
			}

			@Override
			public Prepared prepare(String prompt, ContentInput input) {
				return new Prepared(prompt, new ImageDraft(((ImageInput) input).imageId()));
			}
		};
		TeacherQuestionService extensible = new TeacherQuestionService(
				subjects, questions, new QuestionFormRegistry(List.of(handler)));
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.insert(SUBJECT_ID, "image_test", "Localise", new ImageDraft("media-42")))
				.thenReturn(new StoredQuestion(42L, SUBJECT_ID, "image_test", "Localise", new ImageContent("media-42")));

		QuestionLine line = extensible.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "image_test", "Localise", new ImageInput("media-42"));

		assertThat(line.content()).isEqualTo(new ImageContent("media-42"));
		assertThat(line.formLabel()).isEqualTo("Image test");
		verify(questions).ensureForm(SUBJECT_ID, "image_test", "Image test", 8);
	}

	private record ImageInput(String imageId) implements ContentInput {
	}

	private record ImageDraft(String imageId) implements TeacherQuestionPort.DraftContent {
	}

	private record ImageContent(String imageId) implements TeacherQuestionPort.StoredContent {
	}

	@BeforeEach
	void setUp() {
		service = new TeacherQuestionService(subjects, questions, new QuestionFormRegistry(List.of(
				new QcmQuestionForm(), new MemoryQuestionForm(), new OrderQuestionForm(), new TrueFalseQuestionForm())));
	}

	@Test
	void offeredFormsAreMultipleChoiceThenMemoryThenOrderThenTrueFalse() {
		assertThat(service.listForms(TEACHER_ID, "teacher").forms())
				.satisfies(forms -> {
					assertThat(forms).hasSize(4);
					assertThat(forms.get(0).code()).isEqualTo("qcm");
					assertThat(forms.get(0).label()).isEqualTo("Choix multiples");
					assertThat(forms.get(0).sortOrder()).isEqualTo(1);
					assertThat(forms.get(1).code()).isEqualTo("memory");
					assertThat(forms.get(1).label()).isEqualTo("Memory");
					assertThat(forms.get(1).sortOrder()).isEqualTo(2);
					assertThat(forms.get(2).code()).isEqualTo("order");
					assertThat(forms.get(2).label()).isEqualTo("Ordre");
					assertThat(forms.get(2).sortOrder()).isEqualTo(3);
					assertThat(forms.get(3).code()).isEqualTo("vrai_faux");
					assertThat(forms.get(3).label()).isEqualTo("Vrai ou faux");
					assertThat(forms.get(3).sortOrder()).isEqualTo(4);
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
		when(questions.insert(eq(SUBJECT_ID), eq("qcm"), eq("Combien font 2 + 2 ?"), any()))
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
						new ChoiceInput("4", true)),
				List.of(), List.of());

		assertThat(line.prompt()).isEqualTo("Combien font 2 + 2 ?");
		assertThat(line.formCode()).isEqualTo("qcm");
		assertThat(line.formLabel()).isEqualTo("Choix multiples");
		verify(questions).ensureForm(SUBJECT_ID, "qcm", "Choix multiples", 1);

		ArgumentCaptor<TeacherQuestionPort.ChoicesDraft> captor = ArgumentCaptor.forClass(TeacherQuestionPort.ChoicesDraft.class);
		verify(questions).insert(
				eq(SUBJECT_ID), eq("qcm"), eq("Combien font 2 + 2 ?"), captor.capture());
		assertThat(captor.getValue().choices()).containsExactly(
				new ChoiceDraft("3", false),
				new ChoiceDraft("4", true));
	}

	@Test
	void emptyPromptIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "qcm", "  ",
				List.of(new ChoiceInput("3", false), new ChoiceInput("4", true)),
				List.of(), List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.PROMPT_REQUIRED);

		verify(questions, never()).insert(eq(SUBJECT_ID), eq("qcm"), eq("  "), any());
	}

	@Test
	void oneChoiceIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "qcm", "Combien font 2 + 2 ?",
				List.of(new ChoiceInput("4", true), new ChoiceInput(" ", false)),
				List.of(), List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.CHOICES_REQUIRED);
	}

	@Test
	void missingCorrectAnswerIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "qcm", "Combien font 2 + 2 ?",
				List.of(new ChoiceInput("3", false), new ChoiceInput("4", null)),
				List.of(), List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.CORRECT_REQUIRED);
	}

	@Test
	void unknownFormIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "puzzle", "Paires",
				List.of(new ChoiceInput("2", false), new ChoiceInput("4", true)),
				List.of(), List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.FORM_UNKNOWN);

		verify(questions, never()).ensureForm(SUBJECT_ID, "puzzle", "Puzzle", 2);
	}

	@Test
	void updateRewritesTheStatementAndKeepsTheForm() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.findInSubject(SUBJECT_ID, 7L)).thenReturn(Optional.of(saved()));
		when(questions.replace(eq(7L), eq("Combien font 3 + 3 ?"), any()))
				.thenReturn(new StoredQuestion(
						7L,
						SUBJECT_ID,
						"qcm",
						"Combien font 3 + 3 ?",
						List.of(new StoredChoice(3L, "5", false), new StoredChoice(4L, "6", true)),
						List.of(), List.of()));

		QuestionLine line = service.update(
				TEACHER_ID,
				"teacher",
				SUBJECT_ID,
				7L,
				"qcm",
				"  Combien font 3 + 3 ? ",
				List.of(new ChoiceInput("5", false), new ChoiceInput(" ", false), new ChoiceInput("6", true)),
				List.of(), List.of());

		assertThat(line.id()).isEqualTo(7L);
		assertThat(line.prompt()).isEqualTo("Combien font 3 + 3 ?");
		assertThat(line.formCode()).isEqualTo("qcm");
		verify(questions, never()).insert(eq(SUBJECT_ID), eq("qcm"), eq("Combien font 3 + 3 ?"), any());
		verify(questions, never()).ensureForm(SUBJECT_ID, "qcm", "Choix multiples", 1);
	}

	@Test
	void missingQuestionIsNotFound() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.findInSubject(SUBJECT_ID, 8L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.update(
				TEACHER_ID, "teacher", SUBJECT_ID, 8L, "qcm", "Combien font 2 + 2 ?",
				List.of(new ChoiceInput("3", false), new ChoiceInput("4", true)),
				List.of(), List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.QUESTION_MISSING);

		verify(questions, never()).replace(eq(8L), eq("Combien font 2 + 2 ?"), any());
	}

	@Test
	void changingTheFormOfAnExistingQuestionIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.findInSubject(SUBJECT_ID, 7L)).thenReturn(Optional.of(new StoredQuestion(
				7L, SUBJECT_ID, "vrai_faux", "C'est vrai ?", List.of(), List.of(), List.of())));

		assertThatThrownBy(() -> service.update(
				TEACHER_ID, "teacher", SUBJECT_ID, 7L, "qcm", "Combien font 2 + 2 ?",
				List.of(new ChoiceInput("3", false), new ChoiceInput("4", true)),
				List.of(), List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.FORM_LOCKED);

		verify(questions, never()).replace(eq(7L), eq("Combien font 2 + 2 ?"), any());
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

	@Test
	void memoryKeepsTwoPairsAndAllowsABlankPrompt() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.insert(eq(SUBJECT_ID), eq("memory"), eq(""), any()))
				.thenReturn(new StoredQuestion(
						8L,
						SUBJECT_ID,
						"memory",
						"",
						List.of(),
						List.of(
								new TeacherQuestionPort.StoredPair(1L, "2 + 2", "4"),
								new TeacherQuestionPort.StoredPair(2L, "3 + 3", "6")),
						List.of()));

		QuestionLine line = service.create(
				TEACHER_ID,
				"teacher",
				SUBJECT_ID,
				"memory",
				"   ",
				List.of(),
				List.of(
						new PairInput(" 2 + 2 ", " 4 "),
						new PairInput("", "9"),
						new PairInput("3 + 3", "6")),
				List.of());

		assertThat(line.prompt()).isEmpty();
		assertThat(line.formLabel()).isEqualTo("Memory");
		assertThat(line.pairs()).extracting(pair -> pair.questionSide() + "/" + pair.answerSide())
				.containsExactly("2 + 2/4", "3 + 3/6");
		verify(questions).ensureForm(SUBJECT_ID, "memory", "Memory", 2);

		ArgumentCaptor<TeacherQuestionPort.PairsDraft> captor = ArgumentCaptor.forClass(TeacherQuestionPort.PairsDraft.class);
		verify(questions).insert(eq(SUBJECT_ID), eq("memory"), eq(""), captor.capture());
		assertThat(captor.getValue().pairs()).containsExactly(
				new PairDraft("2 + 2", "4"),
				new PairDraft("3 + 3", "6"));
	}

	@Test
	void onePairIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "memory", "",
				List.of(),
				List.of(new PairInput("2 + 2", "4"), new PairInput("3 + 3", " ")),
				List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.PAIRS_REQUIRED);

		verify(questions, never()).insert(eq(SUBJECT_ID), eq("memory"), eq(""), any());
	}

	@Test
	void orderKeepsTwoItemsAndAllowsABlankPrompt() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.insert(eq(SUBJECT_ID), eq("order"), eq(""), any()))
				.thenReturn(new StoredQuestion(
						9L,
						SUBJECT_ID,
						"order",
						"",
						List.of(),
						List.of(),
						List.of(
								new TeacherQuestionPort.StoredItem(1L, "Lundi"),
								new TeacherQuestionPort.StoredItem(2L, "Mardi"))));

		QuestionLine line = service.create(
				TEACHER_ID,
				"teacher",
				SUBJECT_ID,
				"order",
				"   ",
				List.of(),
				List.of(),
				List.of(new ItemInput(" Lundi "), new ItemInput(" "), new ItemInput("Mardi")));

		assertThat(line.prompt()).isEmpty();
		assertThat(line.formLabel()).isEqualTo("Ordre");
		assertThat(line.items()).extracting(item -> item.label()).containsExactly("Lundi", "Mardi");
		verify(questions).ensureForm(SUBJECT_ID, "order", "Ordre", 3);

		ArgumentCaptor<TeacherQuestionPort.ItemsDraft> captor = ArgumentCaptor.forClass(TeacherQuestionPort.ItemsDraft.class);
		verify(questions).insert(eq(SUBJECT_ID), eq("order"), eq(""), captor.capture());
		assertThat(captor.getValue().items()).containsExactly(
				new TeacherQuestionPort.ItemDraft("Lundi"),
				new TeacherQuestionPort.ItemDraft("Mardi"));
	}

	@Test
	void oneItemIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "order", "",
				List.of(),
				List.of(),
				List.of(new ItemInput("Lundi"), new ItemInput(" "))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.ITEMS_REQUIRED);

		verify(questions, never()).insert(eq(SUBJECT_ID), eq("order"), eq(""), any());
	}

	@Test
	void trueFalseKeepsTwoPhrasesAndTheirSides() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.insert(eq(SUBJECT_ID), eq("vrai_faux"), eq(""), any()))
				.thenReturn(new StoredQuestion(
						10L,
						SUBJECT_ID,
						"vrai_faux",
						"",
						new TeacherQuestionPort.StoredStatements(List.of(
								new TeacherQuestionPort.StoredStatement(1L, "2 + 2 = 4", true),
								new TeacherQuestionPort.StoredStatement(2L, "3 + 3 = 5", false)))));

		QuestionLine line = service.create(
				TEACHER_ID,
				"teacher",
				SUBJECT_ID,
				"vrai_faux",
				"   ",
				new StatementsInput(List.of(
						new StatementInput(" 2 + 2 = 4 ", true),
						new StatementInput(" ", false),
						new StatementInput("3 + 3 = 5", false))));

		assertThat(line.prompt()).isEmpty();
		assertThat(line.formLabel()).isEqualTo("Vrai ou faux");
		assertThat(line.statements()).extracting(row -> row.label() + ":" + row.truthful())
				.containsExactly("2 + 2 = 4:true", "3 + 3 = 5:false");
		verify(questions).ensureForm(SUBJECT_ID, "vrai_faux", "Vrai ou faux", 4);

		ArgumentCaptor<TeacherQuestionPort.StatementsDraft> captor =
				ArgumentCaptor.forClass(TeacherQuestionPort.StatementsDraft.class);
		verify(questions).insert(eq(SUBJECT_ID), eq("vrai_faux"), eq(""), captor.capture());
		assertThat(captor.getValue().statements()).containsExactly(
				new TeacherQuestionPort.StatementDraft("2 + 2 = 4", true),
				new TeacherQuestionPort.StatementDraft("3 + 3 = 5", false));
	}

	@Test
	void onePhraseIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "vrai_faux", "",
				new StatementsInput(List.of(new StatementInput("2 + 2 = 4", true), new StatementInput(" ", false)))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.PHRASES_REQUIRED);

		verify(questions, never()).insert(eq(SUBJECT_ID), eq("vrai_faux"), eq(""), any());
	}

	@Test
	void phraseWithoutSideIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "vrai_faux", "",
				new StatementsInput(List.of(
						new StatementInput("2 + 2 = 4", true),
						new StatementInput("3 + 3 = 5", null)))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.SIDE_REQUIRED);

		verify(questions, never()).insert(eq(SUBJECT_ID), eq("vrai_faux"), eq(""), any());
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
				List.of(new StoredChoice(1L, "3", false), new StoredChoice(2L, "4", true)),
				List.of(), List.of());
	}
}
