package com.appv4.auth.application.service.question;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** SCRUM-198 : ce que Valider accepte pour une réponse libre. */
class FreeTextMatchTest {

	@Test
	void parisInAnyCaseOrAccentIsAccepted() {
		assertThat(FreeTextMatch.accepted("Paris", "paris")).isTrue();
		assertThat(FreeTextMatch.accepted("Paris", "PARIS")).isTrue();
		assertThat(FreeTextMatch.accepted("Paris", "  Pâris  ")).isTrue();
	}

	@Test
	void anotherWordAnExtraLetterOrASpaceInTheMiddleIsRefused() {
		assertThat(FreeTextMatch.accepted("Paris", "Lyon")).isFalse();
		assertThat(FreeTextMatch.accepted("Paris", "Parix")).isFalse();
		assertThat(FreeTextMatch.accepted("Paris", "Pa ris")).isFalse();
	}

	@Test
	void emptyAttemptIsNotASuccess() {
		assertThat(FreeTextMatch.accepted("Paris", "")).isFalse();
		assertThat(FreeTextMatch.accepted("Paris", "   ")).isFalse();
		assertThat(FreeTextMatch.accepted("Paris", null)).isFalse();
	}
}
