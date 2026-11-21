package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort.ChildSession;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.result.ChildLearningSubjectsView;
import com.appv4.auth.application.service.question.QuestionFormRegistry;

/**
 * GET /api/ext/subjects lie d'abord les matières de classe manquantes (BACK #227).
 */
class ReadChildLearningSubjectsAutoLinkTest {

	private final ChildSubjectPort subjects = mock(ChildSubjectPort.class);
	private final TeacherQuestionPort questions = mock(TeacherQuestionPort.class);
	private ReadChildLearningSubjectsService service;

	@BeforeEach
	void childSession() {
		var child = mock(ChildLoginPort.class);
		when(child.findActiveById(7L)).thenReturn(Optional.of(new ChildLoginCandidate(7L, "Akira", "hash")));
		var session = mock(ChildSessionTokenPort.class);
		when(session.read("akira")).thenReturn(Optional.of(new ChildSession(7L)));
		service = new ReadChildLearningSubjectsService(
				child,
				session,
				subjects,
				questions,
				new QuestionFormRegistry(List.of()),
				mock(ChildQuestionAttemptPort.class));
	}

	@Test
	void listEnsuresClassLinksBeforeReadingChosenSubjects() {
		when(subjects.linksOf(7L)).thenReturn(List.of(new StoredLink(9L, false)));
		when(subjects.visibleByIds(List.of(9L)))
				.thenReturn(List.of(new CatalogSubject(9L, "Français", "Mme X", 1L, "4e primaire", "fr")));
		when(questions.countBySubjectIds(List.of(9L))).thenReturn(Map.of(9L, 3L));

		ChildLearningSubjectsView view = service.list("akira");

		InOrder order = inOrder(subjects);
		order.verify(subjects).ensureMissingClassLinksForChild(7L);
		order.verify(subjects).linksOf(7L);
		assertThat(view.subjects()).hasSize(1);
		assertThat(view.subjects().get(0).subjectId()).isEqualTo(9L);
		assertThat(view.subjects().get(0).questionCount()).isEqualTo(3L);
	}

	@Test
	void listKeepsExcludedLinksOutAfterEnsure() {
		when(subjects.linksOf(7L)).thenReturn(List.of(new StoredLink(9L, true)));

		ChildLearningSubjectsView view = service.list("akira");

		InOrder order = inOrder(subjects);
		order.verify(subjects).ensureMissingClassLinksForChild(7L);
		order.verify(subjects).linksOf(7L);
		assertThat(view.subjects()).isEmpty();
	}
}
