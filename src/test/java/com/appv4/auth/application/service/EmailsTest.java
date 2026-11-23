package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;

class EmailsTest {

	@Test
	void normalizesWithRootEvenUnderTurkishLocale() {
		Locale previous = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag("tr-TR"));
			assertThat(Emails.requireNormalized("I@Example.COM")).isEqualTo("i@example.com");
		} finally {
			Locale.setDefault(previous);
		}
	}

	@Test
	void blankRejected() {
		assertThatThrownBy(() -> Emails.requireNormalized("  "))
				.isInstanceOf(AuthDomainException.class);
	}
}
