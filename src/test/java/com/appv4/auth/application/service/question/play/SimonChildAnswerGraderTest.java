package com.appv4.auth.application.service.question.play;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredSimon;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredSimonElement;

/** ADMIN-25 / BACK-55 : la suite tirée est réussie seulement si elle est refaite jusqu'au bout. */
class SimonChildAnswerGraderTest {

	private static final List<Integer> ISSUED = List.of(0, 1, 2, 0);

	/** Rouge, bleu, vert, jaune. */
	private static StoredQuestion question() {
		return new StoredQuestion(
				1L,
				2L,
				"simon",
				"",
				new StoredSimon(
						"couleurs",
						List.of(
								new StoredSimonElement(10L, "rouge", "#ff0000"),
								new StoredSimonElement(11L, "bleu", "#0000ff"),
								new StoredSimonElement(12L, "vert", "#00ff00"),
								new StoredSimonElement(13L, "jaune", "#ffff00"))));
	}

	@Test
	void theSameSequenceIsHundredAndPassed() {
		ChildAnswerGrade grade = SimonChildAnswerGrader.grade(question(), ISSUED, List.of(0, 1, 2, 0));

		assertThat(grade.scorePercent()).isEqualTo(100);
		assertThat(grade.passed()).isTrue();
		assertThat(grade.answers()).containsExactly(
				new AnswerLine("simon_step", 10L, null, 10L, true),
				new AnswerLine("simon_step", 11L, null, 11L, true),
				new AnswerLine("simon_step", 12L, null, 12L, true),
				new AnswerLine("simon_step", 10L, null, 10L, true));
	}

	@Test
	void aWrongSecondStepKeepsTheFirstRound() {
		ChildAnswerGrade grade = SimonChildAnswerGrader.grade(question(), ISSUED, List.of(0, 2, 1, 0));

		assertThat(grade.scorePercent()).isEqualTo(25);
		assertThat(grade.passed()).isFalse();
		assertThat(grade.answers()).extracting(AnswerLine::correct).containsExactly(true, false, false, false);
		assertThat(grade.answers().get(1)).isEqualTo(new AnswerLine("simon_step", 11L, null, 12L, false));
	}

	@Test
	void oneWrongLastStepKeepsTheEarlierRounds() {
		ChildAnswerGrade grade = SimonChildAnswerGrader.grade(question(), ISSUED, List.of(0, 1, 2, 3));

		assertThat(grade.scorePercent()).isEqualTo(75);
		assertThat(grade.passed()).isFalse();
	}

	@Test
	void anEmptyOrLongerReplayIsRefused() {
		refused(List.of(0, 1, 2, 0, 1));
		refused(List.of());
		refused(null);
	}

	@Test
	void anUnknownPositionIsRefused() {
		refused(List.of(0, 1, 2, 4));
		refused(List.of(-1, 1, 2, 0));
		refused(Arrays.asList(0, null, 2, 0));
	}

	@Test
	void aQuestionWithoutAnIssuedSequenceIsRefused() {
		StoredQuestion empty = new StoredQuestion(1L, 2L, "simon", "", new StoredSimon("nombres", List.of()));

		assertThatThrownBy(() -> SimonChildAnswerGrader.grade(empty, List.of(), List.of(0, 1)))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(SimonChildAnswerGrader.SEQUENCE_MISSING);
	}

	private static void refused(List<Integer> replayed) {
		assertThatThrownBy(() -> SimonChildAnswerGrader.grade(question(), ISSUED, replayed))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(SimonChildAnswerGrader.BAD_ANSWER);
	}
}
