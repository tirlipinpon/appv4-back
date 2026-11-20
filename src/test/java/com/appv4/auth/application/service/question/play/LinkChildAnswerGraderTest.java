package com.appv4.auth.application.service.question.play;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.LinkMatchAnswer;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredLinks;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

class LinkChildAnswerGraderTest {

	@Test
	void identicalLabelsCanExchangeTheirAssignedTokens() {
		var left = new StoredQuestion(1L, 2L, "liens", "", new StoredLinks(List.of(
				new StoredLink(10L, "Même libellé", "Premier"), new StoredLink(11L, "Même libellé", "Second"))));
		var right = new StoredQuestion(1L, 2L, "liens", "", new StoredLinks(List.of(
				new StoredLink(10L, "Premier", "Même libellé"), new StoredLink(11L, "Second", "Même libellé"))));
		List<LinkMatchAnswer> reversed = List.of(new LinkMatchAnswer(10L, 11L), new LinkMatchAnswer(11L, 10L));
		assertThat(LinkChildAnswerGrader.grade(left, reversed).passed()).isTrue();
		assertThat(LinkChildAnswerGrader.grade(right, reversed).passed()).isTrue();
	}

	private static StoredQuestion question() {
		return new StoredQuestion(
				1L,
				2L,
				"liens",
				"Relie.",
				new StoredLinks(List.of(
						new StoredLink(1L, "Pomme", "Fruit"),
						new StoredLink(2L, "Chien", "Animal"),
						new StoredLink(3L, "Rouge", "Couleur"))));
	}

	@Test
	void allCorrectYieldsHundredAndPassed() {
		ChildAnswerGrade grade = LinkChildAnswerGrader.grade(
				question(),
				List.of(
						new LinkMatchAnswer(1L, 1L),
						new LinkMatchAnswer(2L, 2L),
						new LinkMatchAnswer(3L, 3L)));
		assertThat(grade.scorePercent()).isEqualTo(100);
		assertThat(grade.passed()).isTrue();
		assertThat(grade.answers()).hasSize(3);
	}

	@Test
	void partialScoreRoundsPercent() {
		ChildAnswerGrade grade = LinkChildAnswerGrader.grade(
				question(),
				List.of(
						new LinkMatchAnswer(1L, 1L),
						new LinkMatchAnswer(2L, 3L),
						new LinkMatchAnswer(3L, 2L)));
		assertThat(grade.scorePercent()).isEqualTo(33);
		assertThat(grade.passed()).isFalse();
	}

	@Test
	void duplicateRightIsRefused() {
		assertThatThrownBy(() -> LinkChildAnswerGrader.grade(
						question(),
						List.of(
								new LinkMatchAnswer(1L, 1L),
								new LinkMatchAnswer(2L, 1L),
								new LinkMatchAnswer(3L, 2L))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LinkChildAnswerGrader.BAD_ANSWER);
	}

	@Test
	void missingLinkIsRefused() {
		assertThatThrownBy(() -> LinkChildAnswerGrader.grade(
						question(),
						List.of(new LinkMatchAnswer(1L, 1L), new LinkMatchAnswer(2L, 2L))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LinkChildAnswerGrader.BAD_ANSWER);
	}
}
