package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;

class DisplayNamesTest {

	@Test
	void signupRejectsTooLong() {
		String tooLong = "x".repeat(DisplayNames.MAX_LENGTH + 1);
		assertThatThrownBy(() -> DisplayNames.requireForSignup(tooLong, "a@b.com"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("trop long");
	}

	@Test
	void providerTruncates() {
		String tooLong = "y".repeat(DisplayNames.MAX_LENGTH + 10);
		String result = DisplayNames.forProvider(tooLong, "a@b.com");
		assertThat(result).hasSize(DisplayNames.MAX_LENGTH);
	}

	@Test
	void fallsBackToEmailLocalPart() {
		assertThat(DisplayNames.requireForSignup("  ", "marie@example.com")).isEqualTo("marie");
	}
}
