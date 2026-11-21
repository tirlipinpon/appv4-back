package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredMark;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSessionTokenPort.ChildSession;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.QuestionSummary;
import com.appv4.auth.application.result.ChildLearningQuestionsView;
import com.appv4.auth.application.result.ChildLearningQuestionsView.QuestionCard;
import com.appv4.auth.application.service.question.QuestionFormRegistry;

/**
 * La liste des questions d'une matière liée renvoie l'étoile déjà enregistrée.
 */
class ChildLearningQuestionMarkTest {

	private final ChildQuestionAttemptPort attempts = mock(ChildQuestionAttemptPort.class);
	private final TeacherQuestionPort questions = mock(TeacherQuestionPort.class);
	private ReadChildLearningSubjectsService list;

	@BeforeEach
	void linkedSubject() {
		var child = mock(ChildLoginPort.class);
		when(child.findActiveById(7L)).thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "hash")));
		var session = mock(ChildSessionTokenPort.class);
		when(session.read("lea")).thenReturn(Optional.of(new ChildSession(7L)));
		var subjects = mock(ChildSubjectPort.class);
		when(subjects.linksOf(7L)).thenReturn(List.of(new StoredLink(4L, false)));
		when(subjects.visibleByIds(List.of(4L)))
				.thenReturn(List.of(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math")));
		when(questions.findSummariesBySubject(4L, 0L, 4)).thenReturn(List.of(
				new QuestionSummary(11L, "qcm", "Sans étoile"),
				new QuestionSummary(12L, "qcm", "Déjà jaune"),
				new QuestionSummary(13L, "qcm", "Ratée")));
		when(attempts.marksFor(eq(7L), any())).thenReturn(Map.of(
				12L, new StoredMark("yellow", 3),
				13L, new StoredMark("black", 0)));
		list = new ReadChildLearningSubjectsService(
				child, session, subjects, questions, new QuestionFormRegistry(List.of()), attempts);
	}

	@Test
	void laListeRendAucuneEtoileNoireOuJauneAvecSonCompte() {
		ChildLearningQuestionsView view = list.questions("lea", 4L, 0L, 3);

		QuestionCard unmarked = view.questions().get(0);
		QuestionCard yellow = view.questions().get(1);
		QuestionCard black = view.questions().get(2);
		assertThat(unmarked.questionId()).isEqualTo(11L);
		assertThat(unmarked.mark()).isEqualTo("none");
		assertThat(unmarked.yellowCount()).isZero();
		assertThat(yellow.mark()).isEqualTo("yellow");
		assertThat(yellow.yellowCount()).isEqualTo(3);
		assertThat(black.mark()).isEqualTo("black");
		assertThat(black.yellowCount()).isZero();
	}
}
