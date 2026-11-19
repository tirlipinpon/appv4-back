package com.appv4.auth.application.service.question.play;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredFreeText;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;

class FreeTextChildAnswerGraderTest {

	private static StoredQuestion question() {
		return new StoredQuestion(9L, 4L, "texte", "Capitale de la France", new StoredFreeText("Paris"));
	}

	@Test
	void acceptsCaseAccentsAndEdgeSpaces() {
		assertPassed("paris");
		assertPassed("PARIS");
		assertPassed("  Pâris  ");
	}

	@Test
	void rejectsAnotherWordATypoOrASpaceInTheMiddle() {
		assertFailed("Lyon");
		assertFailed("Parix");
		assertFailed("Pa ris");
	}

	@Test
	void blankAttemptIsRefused() {
		assertThatThrownBy(() -> FreeTextChildAnswerGrader.grade(question(), "   "))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(FreeTextChildAnswerGrader.BAD_ANSWER);
	}

	private static void assertPassed(String attempt) {
		ChildAnswerGrade grade = FreeTextChildAnswerGrader.grade(question(), attempt);
		assertThat(grade.scorePercent()).isEqualTo(100);
		assertThat(grade.passed()).isTrue();
		assertThat(grade.answers()).hasSize(1);
		assertThat(grade.answers().getFirst().targetKind()).isEqualTo("texte");
		assertThat(grade.answers().getFirst().correct()).isTrue();
	}

	private static void assertFailed(String attempt) {
		ChildAnswerGrade grade = FreeTextChildAnswerGrader.grade(question(), attempt);
		assertThat(grade.scorePercent()).isZero();
		assertThat(grade.passed()).isFalse();
		assertThat(grade.answers().getFirst().correct()).isFalse();
	}
}
