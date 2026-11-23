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

	private static StoredQuestion twoWords() {
		return new StoredQuestion(1L, 2L, "reponse_libre", "Ouvre la bouteille.", new StoredFreeText("tire bouchon"));
	}

	@Test
	void matchingWordPassesIgnoringCase() {
		ChildAnswerGrade grade = LetterChildAnswerGrader.grade(question(), "ÉCOLE");
		assertThat(grade.scorePercent()).isEqualTo(100);
		assertThat(grade.passed()).isTrue();
	}

	@Test
	void missingAccentScoresPartialLetters() {
		// e c o l e vs é c o l e → 4 / 5
		ChildAnswerGrade grade = LetterChildAnswerGrader.grade(question(), "ecole");
		assertThat(grade.scorePercent()).isEqualTo(80);
		assertThat(grade.passed()).isFalse();
	}

	@Test
	void oneWrongLetterInTwoWordsScoresTenOverEleven() {
		ChildAnswerGrade grade = LetterChildAnswerGrader.grade(twoWords(), "tirx bouchon");
		assertThat(grade.scorePercent()).isEqualTo(91);
		assertThat(grade.passed()).isFalse();
	}

	@Test
	void wrongLengthIsRefused() {
		assertThatThrownBy(() -> LetterChildAnswerGrader.grade(question(), "eco"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LetterChildAnswerGrader.BAD_ANSWER);
	}

	@Test
	void aRoundedPercentNeverTurnsAnErrorIntoAWin() {
		for (int length : new int[] {200, 500}) {
			StoredQuestion longWord = new StoredQuestion(
					1L, 2L, "reponse_libre", "Écris le mot.", new StoredFreeText("a".repeat(length)));
			ChildAnswerGrade partial = LetterChildAnswerGrader.grade(longWord, "a".repeat(length - 1) + "b");
			assertThat(partial.scorePercent()).isEqualTo(99);
			assertThat(partial.passed()).isFalse();
			assertThat(LetterChildAnswerGrader.grade(longWord, "a".repeat(length)).passed()).isTrue();
		}
	}

	@Test
	void blankAttemptIsRefused() {
		assertThatThrownBy(() -> LetterChildAnswerGrader.grade(question(), "   "))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LetterChildAnswerGrader.BAD_ANSWER);
	}
}
