package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.appv4.auth.TestPassword;
import com.appv4.auth.application.exception.AuthDomainException;

class PasswordRulesTest {

	@Test
	void acceptsPasswordWithinBcryptLimit() {
		assertThatCode(() -> PasswordRules.validateOrThrow(TestPassword.VALID))
				.doesNotThrowAnyException();
	}

	@Test
	void rejectsPasswordOver72Utf8Bytes() {
		String tooLong = TestPassword.VALID + "a".repeat(65);
		assertThatThrownBy(() -> PasswordRules.validateOrThrow(tooLong))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("trop long");
	}

	@Test
	void rejectsMultibytePasswordOver72BytesWithFewerChars() {
		// "é" = 2 octets UTF-8 → 8 + 33*2 = 74 octets, < 72 caractères
		String password = TestPassword.VALID + "é".repeat(33);
		assertThatThrownBy(() -> PasswordRules.validateOrThrow(password))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("trop long");
	}
}
