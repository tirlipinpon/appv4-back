package com.appv4.auth.application.service.question.play;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredFreeText;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

class LetterChildAnswerGraderTest {

	private static StoredQuestion question() {
		return new StoredQuestion(1L, 2L, "reponse_libre", "Écris le mot.", new StoredFreeText("école"));
	}

	@Test
	void matchingWordPassesIgnoringCase() {
		ChildAnswerGrade grade = LetterChildAnswerGrader.grade(question(), "ÉCOLE");
		assertThat(grade.scorePercent()).isEqualTo(100);
		assertThat(grade.passed()).isTrue();
	}

	@Test
	void missingAccentFails() {
		ChildAnswerGrade grade = LetterChildAnswerGrader.grade(question(), "ecole");
		assertThat(grade.scorePercent()).isEqualTo(0);
		assertThat(grade.passed()).isFalse();
	}

	@Test
	void wrongLengthIsRefused() {
		assertThatThrownBy(() -> LetterChildAnswerGrader.grade(question(), "eco"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LetterChildAnswerGrader.BAD_ANSWER);
	}

	@Test
	void blankAttemptIsRefused() {
		assertThatThrownBy(() -> LetterChildAnswerGrader.grade(question(), "   "))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LetterChildAnswerGrader.BAD_ANSWER);
	}
}
