package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.NewAttempt;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.PageAttempt;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredMark;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort.ChildSession;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.QuestionMark;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.QuestionSummary;
import com.appv4.auth.application.service.question.QuestionFormRegistry;
import com.appv4.auth.application.result.ChildLessonView;
import com.appv4.auth.application.result.ChildLessonView.Question;
import com.appv4.auth.application.result.ChildSubjectBoardView;

class ChildSubjectLessonTest {

	private final ChildLoginPort children = mock(ChildLoginPort.class);
	private final ChildSessionTokenPort session = mock(ChildSessionTokenPort.class);
	private final ChildSubjectPort subjects = mock(ChildSubjectPort.class);
	private final ChildSubjectBoardPort board = mock(ChildSubjectBoardPort.class);
	private final TeacherQuestionPort questions = mock(TeacherQuestionPort.class);
	private final ChildQuestionAttemptPort attempts = mock(ChildQuestionAttemptPort.class);
	private final QuestionFormRegistry forms = mock(QuestionFormRegistry.class);
	private ReadChildSubjectBoardService service;

	@BeforeEach
	void leaLieeAMaths() {
		when(session.read("lea")).thenReturn(Optional.of(new ChildSession(7L)));
		when(children.findActiveById(7L)).thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "hash")));
		when(subjects.linksOf(7L)).thenReturn(List.of(new StoredLink(4L, false)));
		when(subjects.visibleByIds(eq(List.of(4L))))
				.thenReturn(List.of(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math")));
		when(board.findMeta(anyLong())).thenReturn(Optional.empty());
		when(board.topicsOf(anyLong())).thenReturn(List.of());
		when(board.lessonsOfTopics(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
		when(board.questionsOfLessons(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
		when(board.passedQuestionIds(anyLong(), anyLong())).thenReturn(java.util.Set.of());
		when(board.xpEarned(anyLong(), anyLong())).thenReturn(0);
		when(questions.findSummariesBySubject(eq(4L), eq(0L), anyInt())).thenReturn(List.of(
				new QuestionSummary(11L, "qcm", "Combien font 2 + 2 ?"),
				new QuestionSummary(12L, "qcm", "Combien font 3 + 1 ?")));
		when(questions.findSummariesBySubject(eq(4L), eq(12L), anyInt())).thenReturn(List.of());
		when(attempts.pageAttempts(7L, 4L)).thenReturn(List.of());
		when(attempts.marksFor(eq(7L), org.mockito.ArgumentMatchers.any())).thenReturn(java.util.Map.of());
		when(forms.labelOf("qcm")).thenReturn("Choix multiples");
		service = new ReadChildSubjectBoardService(
				children, session, subjects, board, new ChildLessonMembership(questions, attempts, forms));
	}

	@Test
	void deuxQuestionsJamaisTenteesRestentDeLaPlusAncienneALaPlusRecente() {
		ChildSubjectBoardView view = service.board("lea", 4L);

		assertThat(view.lesson().remainingCount()).isEqualTo(2);
		assertThat(view.lesson().questions())
				.extracting(
						Question::questionId, Question::formCode, Question::prompt, Question::formLabel,
						Question::iconCode, Question::color, Question::mark, Question::yellowCount)
				.containsExactly(
						tuple(11L, "qcm", "Combien font 2 + 2 ?", "Choix multiples", "checklist", "#4c6fff",
								QuestionMark.NONE, 0),
						tuple(12L, "qcm", "Combien font 3 + 1 ?", "Choix multiples", "checklist", "#4c6fff",
								QuestionMark.NONE, 0));
	}

	@Test
	void unePageEncoreOuverteLaisseLaQuestion() {
		when(attempts.pageAttempts(7L, 4L)).thenReturn(List.of(
				legacyAttempt(false, 1)));

		assertThat(ids()).containsExactly(11L, 12L);
	}

	@Test
	void unePageRateeJusquAuBoutLaisseLaQuestionAvecSonEtoileNoire() {
		when(attempts.pageAttempts(7L, 4L)).thenReturn(List.of(
				legacyAttempt(false, 0)));
		when(attempts.marksFor(eq(7L), org.mockito.ArgumentMatchers.any()))
				.thenReturn(Map.of(11L, new StoredMark(QuestionMark.BLACK, 0)));

		ChildLessonView lesson = service.board("lea", 4L).lesson();

		assertThat(lesson.questions())
				.extracting(Question::questionId, Question::mark, Question::yellowCount)
				.containsExactly(tuple(11L, QuestionMark.BLACK, 0), tuple(12L, QuestionMark.NONE, 0));
	}

	@Test
	void unePageAccepteeAuDeuxiemeEssaiSortUneQuestionJamaisRatee() {
		when(attempts.pageAttempts(7L, 4L)).thenReturn(List.of(
				legacyAttempt(false, 1),
				legacyAttempt(true, 0)));

		assertThat(ids()).containsExactly(12L);
	}

	@Test
	void uneSeulePageAccepteeApresUnRateLaLaisse() {
		when(attempts.pageAttempts(7L, 4L)).thenReturn(List.of(
				legacyAttempt(false, 0),
				legacyAttempt(true, 0)));

		assertThat(ids()).containsExactly(11L, 12L);
	}

	@Test
	void deuxPagesAccepteesApresUnRateLaSortentMemeAvecUnePageOuverteEntreElles() {
		when(attempts.pageAttempts(7L, 4L)).thenReturn(List.of(
				legacyAttempt(false, 0),
				legacyAttempt(true, 0),
				legacyAttempt(false, 2),
				legacyAttempt(true, 0)));

		assertThat(ids()).containsExactly(12L);
	}

	@Test
	void uneNouvellePageRateeRemetLaQuestionEtGardeLEtoileJaune() {
		when(attempts.pageAttempts(7L, 4L)).thenReturn(List.of(
				legacyAttempt(true, 2),
				legacyAttempt(false, 0)));
		when(attempts.marksFor(eq(7L), org.mockito.ArgumentMatchers.any()))
				.thenReturn(Map.of(11L, new StoredMark(QuestionMark.YELLOW, 3)));

		ChildLessonView lesson = service.board("lea", 4L).lesson();

		assertThat(lesson.questions())
				.extracting(Question::questionId, Question::mark, Question::yellowCount)
				.containsExactly(tuple(11L, QuestionMark.YELLOW, 3), tuple(12L, QuestionMark.NONE, 0));
	}

	@Test
	void uneFormePasEncoreJouableResteDehors() {
		when(questions.findSummariesBySubject(eq(4L), eq(0L), anyInt())).thenReturn(List.of(
				new QuestionSummary(11L, "qcm", "Combien font 2 + 2 ?"),
				new QuestionSummary(13L, "dictee", "Écoute et écris")));
		when(questions.findSummariesBySubject(eq(4L), eq(13L), anyInt())).thenReturn(List.of());

		assertThat(ids()).containsExactly(11L);
	}

	@Test
	void uneMatiereSansQuestionRenvoieUnCompteZero() {
		when(questions.findSummariesBySubject(eq(4L), eq(0L), anyInt())).thenReturn(List.of());

		ChildLessonView lesson = service.board("lea", 4L).lesson();

		assertThat(lesson.remainingCount()).isZero();
		assertThat(lesson.questions()).isEmpty();
	}

	@Test
	void relireLaLeconNEcritRien() {
		service.board("lea", 4L);
		service.board("lea", 4L);

		verify(attempts, never()).insert(org.mockito.ArgumentMatchers.any(NewAttempt.class));
		assertThat(ids()).containsExactly(11L, 12L);
	}

	private List<Long> ids() {
		return service.board("lea", 4L).lesson().questions().stream().map(Question::questionId).toList();
	}

	private static PageAttempt legacyAttempt(boolean passed, int remaining) {
		return new PageAttempt(11L, null, passed, remaining);
	}
}
