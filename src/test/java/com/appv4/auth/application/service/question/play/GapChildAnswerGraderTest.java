package com.appv4.auth.application.service.question.play;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.GapFillAnswer;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredGapWord;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredGaps;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

class GapChildAnswerGraderTest {

	private static StoredQuestion question() {
		return new StoredQuestion(
				1L,
				2L,
				"case_vide",
				"Complète.",
				new StoredGaps(
						"Le ___ mange une ___.",
						List.of(new StoredGapWord(10L, "chat"), new StoredGapWord(11L, "pomme")),
						List.of(new StoredGapWord(12L, "voiture"))));
	}

	@Test
	void allCorrectYieldsHundredAndPassed() {
		ChildAnswerGrade grade = GapChildAnswerGrader.grade(
				question(),
				List.of(new GapFillAnswer(0, 10L), new GapFillAnswer(1, 11L)));
		assertThat(grade.scorePercent()).isEqualTo(100);
		assertThat(grade.passed()).isTrue();
		assertThat(grade.answers()).hasSize(2);
	}

	@Test
	void oneHoleWrongYieldsFifty() {
		ChildAnswerGrade grade = GapChildAnswerGrader.grade(
				question(),
				List.of(new GapFillAnswer(0, 10L), new GapFillAnswer(1, 12L)));
		assertThat(grade.scorePercent()).isEqualTo(50);
		assertThat(grade.passed()).isFalse();
	}

	@Test
	void wordsWithTheSameLabelAreInterchangeable() {
		var question = new StoredQuestion(1L, 2L, "case_vide", "", new StoredGaps(
				"Le ___ rejoint le ___.", List.of(new StoredGapWord(10L, "chat"), new StoredGapWord(11L, "chat")),
				List.of(new StoredGapWord(12L, "chien"))));
		assertThat(GapChildAnswerGrader.grade(question, List.of(new GapFillAnswer(0, 11L), new GapFillAnswer(1, 10L)))
				.passed()).isTrue();
		assertThat(GapChildAnswerGrader.grade(question, List.of(new GapFillAnswer(0, 12L), new GapFillAnswer(1, 10L)))
				.scorePercent()).isEqualTo(50);
	}

	@Test
	void duplicateWordIsRefused() {
		assertThatThrownBy(() -> GapChildAnswerGrader.grade(
						question(),
						List.of(new GapFillAnswer(0, 10L), new GapFillAnswer(1, 10L))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(GapChildAnswerGrader.BAD_ANSWER);
	}

	@Test
	void unknownWordIsRefused() {
		assertThatThrownBy(() -> GapChildAnswerGrader.grade(
						question(),
						List.of(new GapFillAnswer(0, 10L), new GapFillAnswer(1, 99L))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(GapChildAnswerGrader.BAD_ANSWER);
	}
}
