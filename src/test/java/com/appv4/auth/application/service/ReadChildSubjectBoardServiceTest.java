package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort.ChildSession;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort.BoardMeta;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort.LessonQuestion;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort.StoredLesson;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort.StoredTopic;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.result.ChildSubjectBoardView;
import com.appv4.auth.application.service.question.QuestionFormRegistry;

class ReadChildSubjectBoardServiceTest {

	private final ChildLoginPort childLoginPort = Mockito.mock(ChildLoginPort.class);
	private final ChildSessionTokenPort childSessionTokenPort = Mockito.mock(ChildSessionTokenPort.class);
	private final ChildSubjectPort childSubjectPort = Mockito.mock(ChildSubjectPort.class);
	private final ChildSubjectBoardPort boardPort = Mockito.mock(ChildSubjectBoardPort.class);
	private final TeacherQuestionPort teacherQuestionPort = Mockito.mock(TeacherQuestionPort.class);
	private final ChildQuestionAttemptPort childQuestionAttemptPort = Mockito.mock(ChildQuestionAttemptPort.class);
	private ReadChildSubjectBoardService service;

	@BeforeEach
	void setUp() {
		when(childSessionTokenPort.read("token")).thenReturn(Optional.of(new ChildSession(7L)));
		when(childLoginPort.findActiveById(7L)).thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "x")));
		service = new ReadChildSubjectBoardService(
				childLoginPort, childSessionTokenPort, childSubjectPort, boardPort,
				new ChildLessonMembership(
						teacherQuestionPort, childQuestionAttemptPort, Mockito.mock(QuestionFormRegistry.class)));
	}

	@Test
	void boardReturnsProgressContinueAndTopics() {
		when(childSubjectPort.linksOf(7L)).thenReturn(List.of(new StoredLink(5L, false)));
		when(childSubjectPort.visibleByIds(eq(List.of(5L))))
				.thenReturn(List.of(new CatalogSubject(5L, "Maths", "Mme X", 1L, "CE1", "math")));
		when(boardPort.findMeta(5L)).thenReturn(Optional.of(new BoardMeta(
				"Nombres et formes", "On compte", "MONDE MATHS", "5–9 ans", "Facile")));
		when(boardPort.topicsOf(5L)).thenReturn(List.of(
				new StoredTopic(10L, 5L, "Nombres", "math", 0),
				new StoredTopic(11L, 5L, "Formes", "art", 1)));
		when(boardPort.lessonsOfTopics(any())).thenReturn(List.of(
				new StoredLesson(22L, 11L, "Triangles", 0, 4, 40),
				new StoredLesson(23L, 10L, "Plus tard", 1, 2, 20),
				new StoredLesson(21L, 10L, "Compter jusqu'à 20", 1, 3, 50),
				new StoredLesson(20L, 10L, "Compter jusqu'à 10", 0, 5, 50)));
		when(boardPort.questionsOfLessons(any())).thenReturn(List.of(
				new LessonQuestion(23L, 104L, 0),
				new LessonQuestion(20L, 100L, 0),
				new LessonQuestion(21L, 101L, 0),
				new LessonQuestion(21L, 102L, 1),
				new LessonQuestion(22L, 103L, 0)));
		when(boardPort.passedQuestionIds(7L, 5L)).thenReturn(Set.of(100L, 101L));
		when(boardPort.xpEarned(7L, 5L)).thenReturn(120);

		ChildSubjectBoardView view = service.board("token", 5L);

		assertThat(view.name()).isEqualTo("Maths");
		assertThat(view.worldLabel()).isEqualTo("MONDE MATHS");
		assertThat(view.lessonsCompleted()).isEqualTo(1);
		assertThat(view.lessonsTotal()).isEqualTo(4);
		assertThat(view.minutesLeft()).isEqualTo(9);
		assertThat(view.xpEarned()).isEqualTo(120);
		assertThat(view.continueLesson()).isNotNull();
		assertThat(view.continueLesson().title()).isEqualTo("Compter jusqu'à 20");
		assertThat(view.continueLesson().progressPercent()).isEqualTo(50);
		assertThat(view.continueLesson().playQuestionId()).isEqualTo(102L);
		assertThat(view.topics()).hasSize(2);
		assertThat(view.topics().get(0).lessonsCompleted()).isEqualTo(1);
		assertThat(view.topics().get(0).lessonsTotal()).isEqualTo(3);
		assertThat(view.topics().get(1).lessonsCompleted()).isZero();
	}

	@Test
	void boardWithoutLessonsHasNoContinue() {
		when(childSubjectPort.linksOf(7L)).thenReturn(List.of(new StoredLink(5L, false)));
		when(childSubjectPort.visibleByIds(any()))
				.thenReturn(List.of(new CatalogSubject(5L, "Maths", "Mme X", 1L, "CE1", "math")));
		when(boardPort.findMeta(anyLong())).thenReturn(Optional.empty());
		when(boardPort.topicsOf(5L)).thenReturn(List.of());
		when(boardPort.lessonsOfTopics(any())).thenReturn(List.of());
		when(boardPort.questionsOfLessons(any())).thenReturn(List.of());
		when(boardPort.passedQuestionIds(anyLong(), anyLong())).thenReturn(Set.of());
		when(boardPort.xpEarned(anyLong(), anyLong())).thenReturn(0);

		ChildSubjectBoardView view = service.board("token", 5L);

		assertThat(view.continueLesson()).isNull();
		assertThat(view.lessonsTotal()).isZero();
		assertThat(view.topics()).isEmpty();
	}
}
