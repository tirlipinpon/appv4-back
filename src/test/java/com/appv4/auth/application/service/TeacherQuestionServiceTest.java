package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Base64;
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
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ClickImageInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ClickPointInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ClickZoneInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.GapWordInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.GapsInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ItemInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.LinkInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.LinksInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.PairInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.PuzzleInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.PuzzlePieceInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.StatementInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.StatementsInput;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.FollowingChild;
import com.appv4.auth.application.port.out.ChildSubjectPort.SubjectChildLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ChoiceDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickImageDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickPoint;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PairDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.PuzzleDraft;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoice;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickImage;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickZone;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPuzzle;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPuzzlePiece;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.service.question.QuestionFormRegistry;
import com.appv4.auth.application.service.question.QcmQuestionForm;
import com.appv4.auth.application.service.question.MemoryQuestionForm;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.FreeTextInput;
import com.appv4.auth.application.service.question.CaseVideQuestionForm;
import com.appv4.auth.application.service.question.ClickImageQuestionForm;
import com.appv4.auth.application.service.question.PuzzleQuestionForm;
import com.appv4.auth.application.service.question.TexteQuestionForm;
import com.appv4.auth.application.service.question.LinkQuestionForm;
import com.appv4.auth.application.service.question.OrderQuestionForm;
import com.appv4.auth.application.service.question.TrueFalseQuestionForm;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.result.TeacherSubjectFollowersView;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView;
import com.appv4.auth.application.result.TeacherSubjectQuestionsView.QuestionLine;

@ExtendWith(MockitoExtension.class)
class TeacherQuestionServiceTest {

	private static final long TEACHER_ID = 9L;
	private static final long SUBJECT_ID = 4L;

	@Mock
	private TeacherSubjectPort subjects;

	@Mock
	private TeacherQuestionPort questions;

	@Mock
	private ChildSubjectPort children;

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
				subjects, questions, children, new QuestionFormRegistry(List.of(handler)));
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
		service = new TeacherQuestionService(subjects, questions, children, new QuestionFormRegistry(List.of(
				new QcmQuestionForm(), new MemoryQuestionForm(), new OrderQuestionForm(),
				new TrueFalseQuestionForm(), new LinkQuestionForm(), new CaseVideQuestionForm(),
				new ClickImageQuestionForm(), new PuzzleQuestionForm(), new TexteQuestionForm())));
	}

	@Test
	void offeredFormsAreMultipleChoiceThenMemoryThenOrderThenTrueFalse() {
		assertThat(service.listForms(TEACHER_ID, "teacher").forms())
				.satisfies(forms -> {
					assertThat(forms).hasSize(9);
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
					assertThat(forms.get(4).code()).isEqualTo("liens");
					assertThat(forms.get(4).label()).isEqualTo("Liens");
					assertThat(forms.get(4).sortOrder()).isEqualTo(5);
					assertThat(forms.get(5).code()).isEqualTo("case_vide");
					assertThat(forms.get(5).label()).isEqualTo("Case vide");
					assertThat(forms.get(5).sortOrder()).isEqualTo(6);
					assertThat(forms.get(6).code()).isEqualTo("image_interactive");
					assertThat(forms.get(6).label()).isEqualTo("Image à cliquer");
					assertThat(forms.get(6).sortOrder()).isEqualTo(8);
					assertThat(forms.get(7).code()).isEqualTo("puzzle");
					assertThat(forms.get(7).label()).isEqualTo("Puzzle");
					assertThat(forms.get(7).sortOrder()).isEqualTo(10);
					assertThat(forms.get(8).code()).isEqualTo("texte");
					assertThat(forms.get(8).label()).isEqualTo("Réponse libre");
					assertThat(forms.get(8).sortOrder()).isEqualTo(11);
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
				TEACHER_ID, "teacher", SUBJECT_ID, "simon", "Paires",
				List.of(new ChoiceInput("2", false), new ChoiceInput("4", true)),
				List.of(), List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.FORM_UNKNOWN);

		verify(questions, never()).ensureForm(SUBJECT_ID, "simon", "Simon", 2);
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

	@Test
	void linksKeepTwoCouplesAndAllowABlankPrompt() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.insert(eq(SUBJECT_ID), eq("liens"), eq(""), any()))
				.thenReturn(new StoredQuestion(
						11L,
						SUBJECT_ID,
						"liens",
						"",
						new TeacherQuestionPort.StoredLinks(List.of(
								new TeacherQuestionPort.StoredLink(1L, "Pomme", "Fruit"),
								new TeacherQuestionPort.StoredLink(2L, "Chien", "Animal")))));

		QuestionLine line = service.create(
				TEACHER_ID,
				"teacher",
				SUBJECT_ID,
				"liens",
				"   ",
				new LinksInput(List.of(
						new LinkInput(" Pomme ", " Fruit "),
						new LinkInput(" ", " "),
						new LinkInput("Chien", "Animal"))));

		assertThat(line.prompt()).isEmpty();
		assertThat(line.formLabel()).isEqualTo("Liens");
		assertThat(line.links()).extracting(row -> row.leftLabel() + "/" + row.rightLabel())
				.containsExactly("Pomme/Fruit", "Chien/Animal");
		verify(questions).ensureForm(SUBJECT_ID, "liens", "Liens", 5);

		ArgumentCaptor<TeacherQuestionPort.LinksDraft> captor =
				ArgumentCaptor.forClass(TeacherQuestionPort.LinksDraft.class);
		verify(questions).insert(eq(SUBJECT_ID), eq("liens"), eq(""), captor.capture());
		assertThat(captor.getValue().links()).containsExactly(
				new TeacherQuestionPort.LinkDraft("Pomme", "Fruit"),
				new TeacherQuestionPort.LinkDraft("Chien", "Animal"));
	}

	@Test
	void oneLinkIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "liens", "",
				new LinksInput(List.of(new LinkInput("Pomme", "Fruit"), new LinkInput(" ", " ")))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.LINKS_REQUIRED);

		verify(questions, never()).insert(eq(SUBJECT_ID), eq("liens"), eq(""), any());
	}

	@Test
	void wordWithoutAnswerIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "liens", "",
				new LinksInput(List.of(
						new LinkInput("Pomme", "Fruit"),
						new LinkInput("Voiture", " ")))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.LINK_INCOMPLETE);

		verify(questions, never()).insert(eq(SUBJECT_ID), eq("liens"), eq(""), any());
	}

	@Test
	void gapsKeepTheSentenceTheAnswerAndDropABlankDecoy() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.insert(eq(SUBJECT_ID), eq("case_vide"), eq(""), any()))
				.thenReturn(new StoredQuestion(
						12L,
						SUBJECT_ID,
						"case_vide",
						"",
						new TeacherQuestionPort.StoredGaps(
								"Le chat mange une ___ dans le jardin.",
								List.of(new TeacherQuestionPort.StoredGapWord(1L, "pomme")),
								List.of(new TeacherQuestionPort.StoredGapWord(2L, "voiture")))));

		QuestionLine line = service.create(
				TEACHER_ID,
				"teacher",
				SUBJECT_ID,
				"case_vide",
				"   ",
				new GapsInput(
						" Le chat mange une ___ dans le jardin. ",
						List.of(new GapWordInput(" pomme ")),
						List.of(new GapWordInput(" "), new GapWordInput("voiture"))));

		assertThat(line.prompt()).isEmpty();
		assertThat(line.formLabel()).isEqualTo("Case vide");
		assertThat(line.sentence()).isEqualTo("Le chat mange une ___ dans le jardin.");
		assertThat(line.answers()).extracting(row -> row.label()).containsExactly("pomme");
		assertThat(line.decoys()).extracting(row -> row.label()).containsExactly("voiture");
		verify(questions).ensureForm(SUBJECT_ID, "case_vide", "Case vide", 6);

		ArgumentCaptor<TeacherQuestionPort.GapsDraft> captor =
				ArgumentCaptor.forClass(TeacherQuestionPort.GapsDraft.class);
		verify(questions).insert(eq(SUBJECT_ID), eq("case_vide"), eq(""), captor.capture());
		assertThat(captor.getValue().sentence()).isEqualTo("Le chat mange une ___ dans le jardin.");
		assertThat(captor.getValue().answers()).containsExactly(new TeacherQuestionPort.GapWordDraft("pomme"));
		assertThat(captor.getValue().decoys()).containsExactly(new TeacherQuestionPort.GapWordDraft("voiture"));
	}

	@Test
	void phraseWithoutHoleIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "case_vide", "",
				new GapsInput("Le chat mange une pomme.", List.of(new GapWordInput("pomme")), List.of())))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.GAP_REQUIRED);

		verify(questions, never()).insert(eq(SUBJECT_ID), eq("case_vide"), eq(""), any());
	}

	@Test
	void holeWithoutWordIsRefused() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "case_vide", "",
				new GapsInput(
						"Le chat mange une ___ dans le jardin.",
						List.of(new GapWordInput(" ")),
						List.of(new GapWordInput("voiture")))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.GAP_WORD_REQUIRED);

		verify(questions, never()).insert(eq(SUBJECT_ID), eq("case_vide"), eq(""), any());
	}

	@Test
	void cloneCopiesSeveralQuestionsOntoAnotherSubjectAndKeepsTheOriginals() {
		long targetId = 8L;
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(subjects.findOwned(TEACHER_ID, targetId)).thenReturn(Optional.of(
				new StoredSubject(targetId, TEACHER_ID, 1L, "CE2", "Histoire", true)));
		when(questions.findInSubject(SUBJECT_ID, 7L)).thenReturn(Optional.of(saved()));
		when(questions.findInSubject(SUBJECT_ID, 12L)).thenReturn(Optional.of(gapsQuestion()));
		when(questions.insert(eq(targetId), eq("qcm"), eq("Combien font 2 + 2 ?"), any()))
				.thenReturn(new StoredQuestion(
						20L, targetId, "qcm", "Combien font 2 + 2 ?",
						List.of(new StoredChoice(3L, "3", false), new StoredChoice(4L, "4", true)),
						List.of(), List.of()));
		when(questions.insert(eq(targetId), eq("case_vide"), eq(""), any()))
				.thenReturn(new StoredQuestion(
						21L, targetId, "case_vide", "",
						new TeacherQuestionPort.StoredGaps(
								"Le chat ___",
								List.of(new TeacherQuestionPort.StoredGapWord(5L, "mange")),
								List.of(new TeacherQuestionPort.StoredGapWord(6L, "dort")))));

		TeacherSubjectQuestionsView view = service.cloneQuestions(
				TEACHER_ID, "teacher", SUBJECT_ID, targetId, List.of(7L, 12L, 7L));

		assertThat(view.questions()).extracting(QuestionLine::id).containsExactly(20L, 21L);
		assertThat(view.questions().get(1).sentence()).isEqualTo("Le chat ___");
		verify(questions).ensureForm(targetId, "qcm", "Choix multiples", 1);
		verify(questions).ensureForm(targetId, "case_vide", "Case vide", 6);
		verify(questions, never()).replace(anyLong(), any(), any());

		ArgumentCaptor<TeacherQuestionPort.ChoicesDraft> choices =
				ArgumentCaptor.forClass(TeacherQuestionPort.ChoicesDraft.class);
		verify(questions).insert(eq(targetId), eq("qcm"), eq("Combien font 2 + 2 ?"), choices.capture());
		assertThat(choices.getValue().choices()).containsExactly(
				new ChoiceDraft("3", false),
				new ChoiceDraft("4", true));

		ArgumentCaptor<TeacherQuestionPort.GapsDraft> gaps =
				ArgumentCaptor.forClass(TeacherQuestionPort.GapsDraft.class);
		verify(questions).insert(eq(targetId), eq("case_vide"), eq(""), gaps.capture());
		assertThat(gaps.getValue().sentence()).isEqualTo("Le chat ___");
		assertThat(gaps.getValue().answers()).containsExactly(new TeacherQuestionPort.GapWordDraft("mange"));
		assertThat(gaps.getValue().decoys()).containsExactly(new TeacherQuestionPort.GapWordDraft("dort"));
	}

	@Test
	void cloneOntoTheSameSubjectStillInsertsANewQuestion() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.findInSubject(SUBJECT_ID, 7L)).thenReturn(Optional.of(saved()));
		when(questions.insert(eq(SUBJECT_ID), eq("qcm"), eq("Combien font 2 + 2 ?"), any()))
				.thenReturn(new StoredQuestion(
						20L, SUBJECT_ID, "qcm", "Combien font 2 + 2 ?",
						List.of(new StoredChoice(3L, "3", false), new StoredChoice(4L, "4", true)),
						List.of(), List.of()));

		TeacherSubjectQuestionsView view = service.cloneQuestions(
				TEACHER_ID, "teacher", SUBJECT_ID, SUBJECT_ID, List.of(7L));

		assertThat(view.questions()).extracting(QuestionLine::id).containsExactly(20L);
		verify(questions).insert(eq(SUBJECT_ID), eq("qcm"), eq("Combien font 2 + 2 ?"), any());
	}

	@Test
	void cloneRefusesAnEmptySelectionBeforeTouchingTheSubjects() {
		assertThatThrownBy(() -> service.cloneQuestions(TEACHER_ID, "teacher", SUBJECT_ID, 8L, List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.CLONE_REQUIRED);

		verify(subjects, never()).findOwned(TEACHER_ID, SUBJECT_ID);
		verify(questions, never()).insert(anyLong(), any(), any(), any());
	}

	@Test
	void cloneRefusesAMissingTarget() {
		assertThatThrownBy(() -> service.cloneQuestions(TEACHER_ID, "teacher", SUBJECT_ID, 0L, List.of(7L)))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.CLONE_TARGET);
	}

	@Test
	void cloneRefusesAQuestionThatIsNotOnTheSourceSubject() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(subjects.findOwned(TEACHER_ID, 8L)).thenReturn(Optional.of(
				new StoredSubject(8L, TEACHER_ID, 1L, "CE2", "Histoire", true)));
		when(questions.findInSubject(SUBJECT_ID, 7L)).thenReturn(Optional.of(saved()));
		when(questions.findInSubject(SUBJECT_ID, 99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.cloneQuestions(
				TEACHER_ID, "teacher", SUBJECT_ID, 8L, List.of(7L, 99L)))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.NOT_FOUND);

		verify(questions, never()).insert(anyLong(), any(), any(), any());
	}

	@Test
	void cloneRefusesASubjectTheTeacherDoesNotHave() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(subjects.findOwned(TEACHER_ID, 8L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.cloneQuestions(
				TEACHER_ID, "teacher", SUBJECT_ID, 8L, List.of(7L)))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherSubjectService.SUBJECT_MISSING);

		verify(questions, never()).findInSubject(SUBJECT_ID, 7L);
	}

	@Test
	void parentCannotCloneQuestions() {
		assertThatThrownBy(() -> service.cloneQuestions(TEACHER_ID, "parent", SUBJECT_ID, 8L, List.of(7L)))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.FORBIDDEN);

		verify(questions, never()).insert(anyLong(), any(), any(), any());
	}

	@Test
	void followersListsClassmatesUnlessTheParentRemovedTheCourseAndKeepsAddedChildren() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(children.activeInClass(1L, "CE1")).thenReturn(List.of(
				new FollowingChild(3L, "Léa", "Martin"),
				new FollowingChild(5L, "Tom", "Bernard")));
		when(children.linksOfSubject(SUBJECT_ID)).thenReturn(List.of(
				new SubjectChildLink(new FollowingChild(5L, "Tom", "Bernard"), true),
				new SubjectChildLink(new FollowingChild(8L, "Nina", "Bernard"), false)));

		TeacherSubjectFollowersView view = service.followers(TEACHER_ID, "teacher", SUBJECT_ID);

		assertThat(view.childCount()).isEqualTo(2);
		assertThat(view.names()).containsExactly("Léa Martin", "Nina Bernard");
		assertThat(view.warning()).isEqualTo("2 enfants suivent ce cours.");
	}

	@Test
	void followersWarnsWhenNobodyFollowsTheCourse() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		TeacherSubjectFollowersView view = service.followers(TEACHER_ID, "teacher", SUBJECT_ID);

		assertThat(view.childCount()).isZero();
		assertThat(view.names()).isEmpty();
		assertThat(view.warning()).isEqualTo(TeacherQuestionService.followersWarning(0));
	}

	@Test
	void parentCannotSeeWhoFollowsTheCourse() {
		assertThatThrownBy(() -> service.followers(TEACHER_ID, "parent", SUBJECT_ID))
				.isInstanceOf(AuthDomainException.class)
				.extracting(ex -> ((AuthDomainException) ex).getErrorCode())
				.isEqualTo(AuthErrorCode.FORBIDDEN);

		verify(children, never()).activeInClass(anyLong(), any());
	}

	@Test
	void deleteRemovesEachChosenQuestionOnce() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.findInSubject(SUBJECT_ID, 7L)).thenReturn(Optional.of(saved()));
		when(questions.findInSubject(SUBJECT_ID, 8L)).thenReturn(Optional.of(saved()));

		List<Long> asked = new ArrayList<>();
		asked.add(7L);
		asked.add(7L);
		asked.add(null);
		asked.add(8L);

		assertThat(service.deleteQuestions(TEACHER_ID, "teacher", SUBJECT_ID, asked))
				.containsExactly(7L, 8L);

		verify(questions).delete(7L);
		verify(questions).delete(8L);
		verify(questions, times(2)).delete(anyLong());
	}

	@Test
	void deleteRefusesWhenOneQuestionIsMissingAndRemovesNothing() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.findInSubject(SUBJECT_ID, 7L)).thenReturn(Optional.of(saved()));
		when(questions.findInSubject(SUBJECT_ID, 99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.deleteQuestions(TEACHER_ID, "teacher", SUBJECT_ID, List.of(7L, 99L)))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.QUESTION_MISSING);

		verify(questions, never()).delete(anyLong());
	}

	@Test
	void deleteRefusesAnEmptyChoice() {
		assertThatThrownBy(() -> service.deleteQuestions(TEACHER_ID, "teacher", SUBJECT_ID, List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.DELETE_REQUIRED);

		verify(questions, never()).delete(anyLong());
	}

	@Test
	void freeTextKeepsThePromptAndTheExpectedAnswer() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.insert(eq(SUBJECT_ID), eq("texte"), eq("Capitale de la France"), any()))
				.thenReturn(new StoredQuestion(
						13L,
						SUBJECT_ID,
						"texte",
						"Capitale de la France",
						new TeacherQuestionPort.StoredFreeText("Paris")));

		QuestionLine line = service.create(
				TEACHER_ID,
				"teacher",
				SUBJECT_ID,
				"texte",
				"  Capitale de la France  ",
				new FreeTextInput("  Paris  "));

		assertThat(line.prompt()).isEqualTo("Capitale de la France");
		assertThat(line.formCode()).isEqualTo("texte");
		assertThat(line.formLabel()).isEqualTo("Réponse libre");
		assertThat(line.expectedAnswer()).isEqualTo("Paris");
		verify(questions).ensureForm(SUBJECT_ID, "texte", "Réponse libre", 11);

		ArgumentCaptor<TeacherQuestionPort.FreeTextDraft> captor =
				ArgumentCaptor.forClass(TeacherQuestionPort.FreeTextDraft.class);
		verify(questions).insert(eq(SUBJECT_ID), eq("texte"), eq("Capitale de la France"), captor.capture());
		assertThat(captor.getValue().expectedAnswer()).isEqualTo("Paris");
	}

	@Test
	void freeTextRefusesAnEmptyAnswer() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "texte", "Capitale de la France",
				new FreeTextInput("  ")))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.ANSWER_REQUIRED);

		verify(questions, never()).insert(eq(SUBJECT_ID), eq("texte"), any(), any());
	}

	@Test
	void freeTextRefusesAnEmptyPrompt() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "texte", "  ",
				new FreeTextInput("Paris")))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.PROMPT_REQUIRED);

		verify(questions, never()).insert(eq(SUBJECT_ID), eq("texte"), any(), any());
	}

	@Test
	void freeTextUpdateReplacesTheAnswerAndKeepsTheForm() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.findInSubject(SUBJECT_ID, 13L)).thenReturn(Optional.of(new StoredQuestion(
				13L, SUBJECT_ID, "texte", "Capitale de la France",
				new TeacherQuestionPort.StoredFreeText("Paris"))));
		when(questions.replace(eq(13L), eq("Capitale de l'Italie"), any()))
				.thenReturn(new StoredQuestion(
						13L, SUBJECT_ID, "texte", "Capitale de l'Italie",
						new TeacherQuestionPort.StoredFreeText("Rome")));

		QuestionLine line = service.update(
				TEACHER_ID, "teacher", SUBJECT_ID, 13L, "texte",
				"Capitale de l'Italie", new FreeTextInput("Rome"));

		assertThat(line.formCode()).isEqualTo("texte");
		assertThat(line.expectedAnswer()).isEqualTo("Rome");
		verify(questions).replace(eq(13L), eq("Capitale de l'Italie"), any());
	}

	@Test
	void freeTextCannotChangeForm() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.findInSubject(SUBJECT_ID, 13L)).thenReturn(Optional.of(new StoredQuestion(
				13L, SUBJECT_ID, "texte", "Capitale de la France",
				new TeacherQuestionPort.StoredFreeText("Paris"))));

		assertThatThrownBy(() -> service.update(
				TEACHER_ID, "teacher", SUBJECT_ID, 13L, "qcm",
				"Capitale de la France",
				List.of(new ChoiceInput("Paris", true), new ChoiceInput("Lyon", false)),
				List.of(), List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.FORM_LOCKED);

		verify(questions, never()).replace(anyLong(), any(), any());
	}

	@Test
	void clickImageKeepsTheWebpTheZonesAndTheRule() {
		byte[] webp = tinyWebp();
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.insert(eq(SUBJECT_ID), eq("image_interactive"), eq(""), any()))
				.thenReturn(new StoredQuestion(
						14L,
						SUBJECT_ID,
						"image_interactive",
						"",
						new StoredClickImage(webp, true, List.of(new StoredClickZone(
								1L,
								"le soleil",
								true,
								"polygon",
								List.of(
										new ClickPoint(0.1, 0.2),
										new ClickPoint(0.3, 0.2),
										new ClickPoint(0.2, 0.4)))))));

		QuestionLine line = service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "image_interactive", "  ",
				new ClickImageInput(Base64.getEncoder().encodeToString(webp), true, List.of(
						new ClickZoneInput(" le soleil ", true, "polygon", List.of(
								new ClickPointInput(0.1, 0.2),
								new ClickPointInput(0.3, 0.2),
								new ClickPointInput(0.2, 0.4))),
						new ClickZoneInput("le nuage", false, "rect", List.of(
								new ClickPointInput(0.6, 0.7),
								new ClickPointInput(0.5, 0.55))))));

		assertThat(line.prompt()).isEmpty();
		assertThat(line.formCode()).isEqualTo("image_interactive");
		assertThat(line.formLabel()).isEqualTo("Image à cliquer");
		assertThat(line.requireAllCorrect()).isTrue();
		assertThat(line.imageWebp()).containsExactly(webp);
		assertThat(line.zones()).hasSize(1);
		assertThat(line.zones().get(0).label()).isEqualTo("le soleil");
		assertThat(line.zones().get(0).correct()).isTrue();

		ArgumentCaptor<ClickImageDraft> captor = ArgumentCaptor.forClass(ClickImageDraft.class);
		verify(questions).insert(eq(SUBJECT_ID), eq("image_interactive"), eq(""), captor.capture());
		assertThat(captor.getValue().imageWebp()).containsExactly(webp);
		assertThat(captor.getValue().requireAllCorrect()).isTrue();
		assertThat(captor.getValue().zones()).hasSize(2);
		assertThat(captor.getValue().zones().get(0).label()).isEqualTo("le soleil");
		assertThat(captor.getValue().zones().get(1).shape()).isEqualTo("rect");
		assertThat(captor.getValue().zones().get(1).correct()).isFalse();
		assertThat(captor.getValue().zones().get(1).points().get(0).x()).isEqualTo(0.5);
		assertThat(captor.getValue().zones().get(1).points().get(0).y()).isEqualTo(0.55);
		verify(questions).ensureForm(SUBJECT_ID, "image_interactive", "Image à cliquer", 8);
	}

	@Test
	void clickImageRefusesAnIncompleteDrawing() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		byte[] webp = tinyWebp();
		String encoded = Base64.getEncoder().encodeToString(webp);
		ClickZoneInput sun = new ClickZoneInput("le soleil", true, "polygon", List.of(
				new ClickPointInput(0.1, 0.2), new ClickPointInput(0.3, 0.2), new ClickPointInput(0.2, 0.4)));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "image_interactive", "",
				new ClickImageInput("  ", true, List.of(sun))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.IMAGE_REQUIRED);

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "image_interactive", "",
				new ClickImageInput(encoded, true, List.of())))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.ZONES_REQUIRED);

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "image_interactive", "",
				new ClickImageInput(encoded, null, List.of(sun))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.ZONE_RULE_REQUIRED);

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "image_interactive", "",
				new ClickImageInput(encoded, true, List.of(
						new ClickZoneInput("  ", true, "polygon", sun.points())))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.ZONE_NAME_REQUIRED);

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "image_interactive", "",
				new ClickImageInput(encoded, true, List.of(
						new ClickZoneInput("le soleil", null, "polygon", sun.points())))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.ZONE_SIDE_REQUIRED);

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "image_interactive", "",
				new ClickImageInput(encoded, true, List.of(
						new ClickZoneInput("le soleil", true, "polygon", List.of(
								new ClickPointInput(1.2, 0.2),
								new ClickPointInput(0.3, 0.2),
								new ClickPointInput(0.2, 0.4)))))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.ZONE_POINTS);

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "image_interactive", "",
				new ClickImageInput(Base64.getEncoder().encodeToString(new byte[] {
						(byte) 0x89, 'P', 'N', 'G', 0, 0, 0, 0, 0, 0, 0, 0
				}), true, List.of(sun))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.IMAGE_NOT_WEBP);

		verify(questions, never()).insert(eq(SUBJECT_ID), eq("image_interactive"), any(), any());
	}

	@Test
	void puzzleKeepsTheWebpAndThePiecesWithoutACorrectSide() {
		byte[] webp = tinyWebp();
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		when(questions.insert(eq(SUBJECT_ID), eq("puzzle"), eq(""), any()))
				.thenReturn(new StoredQuestion(
						15L,
						SUBJECT_ID,
						"puzzle",
						"",
						new StoredPuzzle(webp, List.of(
								new StoredPuzzlePiece(1L, "le ciel", "rect", List.of(
										new ClickPoint(0.0, 0.0), new ClickPoint(1.0, 0.5))),
								new StoredPuzzlePiece(2L, "le sol", "rect", List.of(
										new ClickPoint(0.0, 0.5), new ClickPoint(1.0, 1.0)))))));

		QuestionLine line = service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "puzzle", "  ",
				new PuzzleInput(Base64.getEncoder().encodeToString(webp), List.of(
						new PuzzlePieceInput(" le ciel ", "rect", List.of(
								new ClickPointInput(0.0, 0.0), new ClickPointInput(1.0, 0.5))),
						new PuzzlePieceInput("le sol", "ellipse", List.of(
								new ClickPointInput(0.5, 0.75), new ClickPointInput(0.7, 0.9))))));

		assertThat(line.prompt()).isEmpty();
		assertThat(line.formCode()).isEqualTo("puzzle");
		assertThat(line.formLabel()).isEqualTo("Puzzle");
		assertThat(line.requireAllCorrect()).isNull();
		assertThat(line.zones()).isEmpty();
		assertThat(line.imageWebp()).containsExactly(webp);
		assertThat(line.pieces()).hasSize(2);
		assertThat(line.pieces().get(0).label()).isEqualTo("le ciel");

		ArgumentCaptor<PuzzleDraft> captor = ArgumentCaptor.forClass(PuzzleDraft.class);
		verify(questions).insert(eq(SUBJECT_ID), eq("puzzle"), eq(""), captor.capture());
		assertThat(captor.getValue().imageWebp()).containsExactly(webp);
		assertThat(captor.getValue().pieces()).hasSize(2);
		assertThat(captor.getValue().pieces().get(0).label()).isEqualTo("le ciel");
		assertThat(captor.getValue().pieces().get(1).shape()).isEqualTo("ellipse");
		verify(questions).ensureForm(SUBJECT_ID, "puzzle", "Puzzle", 10);
	}

	@Test
	void puzzleRefusesOnePieceOrAMissingName() {
		when(subjects.findOwned(TEACHER_ID, SUBJECT_ID)).thenReturn(Optional.of(owned()));
		byte[] webp = tinyWebp();
		String encoded = Base64.getEncoder().encodeToString(webp);
		PuzzlePieceInput sky = new PuzzlePieceInput("le ciel", "rect", List.of(
				new ClickPointInput(0.0, 0.0), new ClickPointInput(1.0, 0.5)));
		PuzzlePieceInput ground = new PuzzlePieceInput("le sol", "rect", List.of(
				new ClickPointInput(0.0, 0.5), new ClickPointInput(1.0, 1.0)));

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "puzzle", "",
				new PuzzleInput("  ", List.of(sky, ground))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.IMAGE_REQUIRED);

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "puzzle", "",
				new PuzzleInput(encoded, List.of(sky))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.PIECES_REQUIRED);

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "puzzle", "",
				new PuzzleInput(encoded, List.of(sky, new PuzzlePieceInput("  ", "rect", ground.points())))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.PIECE_NAME_REQUIRED);

		assertThatThrownBy(() -> service.create(
				TEACHER_ID, "teacher", SUBJECT_ID, "puzzle", "",
				new PuzzleInput(encoded, List.of(sky, new PuzzlePieceInput("le sol", "star", ground.points())))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherQuestionService.PIECE_SHAPE);

		verify(questions, never()).insert(eq(SUBJECT_ID), eq("puzzle"), any(), any());
	}

	private static byte[] tinyWebp() {
		return Base64.getDecoder().decode("UklGRiQAAABXRUJQVlA4IBgAAAA=");
	}

	private static StoredQuestion gapsQuestion() {
		return new StoredQuestion(
				12L,
				SUBJECT_ID,
				"case_vide",
				"",
				new TeacherQuestionPort.StoredGaps(
						"Le chat ___",
						List.of(new TeacherQuestionPort.StoredGapWord(1L, "mange")),
						List.of(new TeacherQuestionPort.StoredGapWord(2L, "dort"))));
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
