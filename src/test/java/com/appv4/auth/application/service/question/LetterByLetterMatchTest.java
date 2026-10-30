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
}
