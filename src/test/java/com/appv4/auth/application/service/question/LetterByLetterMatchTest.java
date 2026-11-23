package com.appv4.auth.application.service.question;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SCRUM-182 : ce que Valider accepte pour une saisie lettre par lettre. */
class LetterByLetterMatchTest {

	@Test
	void parisInAnyCaseIsAccepted() {
		assertThat(LetterByLetterMatch.accepted("Paris", "paris")).isTrue();
		assertThat(LetterByLetterMatch.accepted("Paris", "PARIS")).isTrue();
		assertThat(LetterByLetterMatch.accepted("Paris", "  Paris  ")).isTrue();
	}

	@Test
	void aDifferentAccentAnotherWordOrASpaceInTheMiddleIsRefused() {
		assertThat(LetterByLetterMatch.accepted("Paris", "Parïs")).isFalse();
		assertThat(LetterByLetterMatch.accepted("Paris", "Parix")).isFalse();
		assertThat(LetterByLetterMatch.accepted("Paris", "Lyon")).isFalse();
		assertThat(LetterByLetterMatch.accepted("Paris", "Pa ris")).isFalse();
		assertThat(LetterByLetterMatch.accepted("Été", "ete")).isFalse();
	}

	@Test
	void theSameAccentInAnotherCaseIsAccepted() {
		assertThat(LetterByLetterMatch.accepted("Été", "été")).isTrue();
		assertThat(LetterByLetterMatch.accepted("Été", "ÉTÉ")).isTrue();
	}

	@Test
	void emptyAttemptIsNotASuccess() {
		assertThat(LetterByLetterMatch.accepted("Paris", "")).isFalse();
		assertThat(LetterByLetterMatch.accepted("Paris", "   ")).isFalse();
		assertThat(LetterByLetterMatch.accepted("Paris", null)).isFalse();
	}

	@Test
	void maskKeepsSpacesWithoutRevealingLetters() {
		assertThat(LetterByLetterMatch.mask("tire bouchon")).isEqualTo("#### #######");
		assertThat(LetterByLetterMatch.mask("Paris")).isEqualTo("#####");
	}

	@Test
	void positionLocksFreezeCorrectLettersAndSpaces() {
		String expected = "tire bouchon";
		String attempt = "tir" + LetterByLetterMatch.EMPTY_SLOT + " b"
				+ String.valueOf(LetterByLetterMatch.EMPTY_SLOT).repeat(6);
		assertThat(LetterByLetterMatch.positionLocks(expected, attempt)).containsExactly(
				true, true, true, false, true, true, false, false, false, false, false, false);
	}

	@Test
	void twoWordAnswerIsAcceptedWithItsSpace() {
		assertThat(LetterByLetterMatch.accepted("tire bouchon", "tire bouchon")).isTrue();
		assertThat(LetterByLetterMatch.accepted("tire bouchon", "TIRE BOUCHON")).isTrue();
		assertThat(LetterByLetterMatch.accepted("tire bouchon", "tirebouchon")).isFalse();
	}

	@Test
	void scorePercentCountsOnlyLetterSlots() {
		assertThat(LetterByLetterMatch.scorePercent("tire bouchon", "tire bouchon")).isEqualTo(100);
		assertThat(LetterByLetterMatch.scorePercent("tire bouchon", "tirx bouchon")).isEqualTo(91);
		assertThat(LetterByLetterMatch.scorePercent("école", "ecole")).isEqualTo(80);
	}
}
