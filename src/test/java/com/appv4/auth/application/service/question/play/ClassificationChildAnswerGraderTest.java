package com.appv4.auth.application.service.question.play;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.PlacementAnswer;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredCard;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClassification;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredColumn;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

class ClassificationChildAnswerGraderTest {

	private static StoredQuestion question() {
		return new StoredQuestion(
				1L,
				2L,
				"classement",
				"Classe.",
				new StoredClassification(
						List.of(new StoredColumn(10L, "Oui"), new StoredColumn(11L, "Non")),
						List.of(
								new StoredCard(1L, "A", 10L),
								new StoredCard(2L, "B", 11L),
								new StoredCard(3L, "C", 10L))));
	}

	@Test
	void allCorrectYieldsHundredAndPassed() {
		ChildAnswerGrade grade = ClassificationChildAnswerGrader.grade(
				question(),
				List.of(
						new PlacementAnswer(1L, 10L),
						new PlacementAnswer(2L, 11L),
						new PlacementAnswer(3L, 10L)));
		assertThat(grade.scorePercent()).isEqualTo(100);
		assertThat(grade.passed()).isTrue();
		assertThat(grade.answers()).hasSize(3);
	}

	@Test
	void partialScoreRoundsPercent() {
		ChildAnswerGrade grade = ClassificationChildAnswerGrader.grade(
				question(),
				List.of(
						new PlacementAnswer(1L, 10L),
						new PlacementAnswer(2L, 10L),
						new PlacementAnswer(3L, 10L)));
		assertThat(grade.scorePercent()).isEqualTo(67);
		assertThat(grade.passed()).isFalse();
	}

	@Test
	void missingCardIsRefused() {
		assertThatThrownBy(() -> ClassificationChildAnswerGrader.grade(
						question(),
						List.of(new PlacementAnswer(1L, 10L), new PlacementAnswer(2L, 11L))))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(ClassificationChildAnswerGrader.BAD_ANSWER);
	}
}
