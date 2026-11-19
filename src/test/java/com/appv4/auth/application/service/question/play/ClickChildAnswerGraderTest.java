package com.appv4.auth.application.service.question.play;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.ZoneClickAnswer;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickPoint;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickImage;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickZone;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

class ClickChildAnswerGraderTest {

	private static StoredQuestion question(boolean ordered, boolean requireAll) {
		return new StoredQuestion(
				1L,
				2L,
				"image_interactive",
				"Trouve-les.",
				new StoredClickImage(
						new byte[]{1, 2},
						requireAll,
						ordered,
						List.of(
								zone(10L, "Clique sur le chat.", true, ordered ? 1 : null),
								zone(11L, "Clique sur la pomme.", true, ordered ? 2 : null),
								zone(12L, "Orange", false, null))));
	}

	private static StoredClickZone zone(long id, String label, boolean correct, Integer order) {
		return new StoredClickZone(
				id, label, correct, "rect", List.of(new ClickPoint(0, 0), new ClickPoint(0.5, 1)), order);
	}

	@Test
	void orderedFullSequencePasses() {
		ChildAnswerGrade grade = ClickChildAnswerGrader.grade(
				question(true, true),
				List.of(new ZoneClickAnswer(10L), new ZoneClickAnswer(11L)));
		assertThat(grade.scorePercent()).isEqualTo(100);
		assertThat(grade.passed()).isTrue();
	}

	@Test
	void orderedWrongStepScoresHalf() {
		ChildAnswerGrade grade = ClickChildAnswerGrader.grade(
				question(true, true),
				List.of(new ZoneClickAnswer(12L), new ZoneClickAnswer(11L)));
		assertThat(grade.scorePercent()).isEqualTo(50);
		assertThat(grade.passed()).isFalse();
	}

	@Test
	void unorderedFindsBothAndIgnoresAnExtraWrongClick() {
		ChildAnswerGrade grade = ClickChildAnswerGrader.grade(
				question(false, true),
				List.of(new ZoneClickAnswer(11L), new ZoneClickAnswer(12L), new ZoneClickAnswer(10L)));
		assertThat(grade.scorePercent()).isEqualTo(100);
		assertThat(grade.passed()).isTrue();
	}

	@Test
	void unorderedOneCorrectFailsWhenAllAreRequired() {
		ChildAnswerGrade grade = ClickChildAnswerGrader.grade(
				question(false, true),
				List.of(new ZoneClickAnswer(10L)));
		assertThat(grade.scorePercent()).isEqualTo(50);
		assertThat(grade.passed()).isFalse();
	}

	@Test
	void unorderedOneCorrectPassesWhenAllAreNotRequired() {
		ChildAnswerGrade grade = ClickChildAnswerGrader.grade(
				question(false, false),
				List.of(new ZoneClickAnswer(10L)));
		assertThat(grade.scorePercent()).isEqualTo(50);
		assertThat(grade.passed()).isTrue();
	}

	@Test
	void unknownZoneIsRefused() {
		assertThatThrownBy(() -> ClickChildAnswerGrader.grade(
						question(false, true),
						List.of(new ZoneClickAnswer(99L))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ClickChildAnswerGrader.BAD_ANSWER);
	}
}
