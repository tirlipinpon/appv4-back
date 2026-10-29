package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;

class AccountContactTest {

	@Test
	void nameTrimsAndRefusesBlankOrTooLong() {
		assertThat(AccountContact.name("  Parent Contact  ")).isEqualTo("Parent Contact");
		assertThatThrownBy(() -> AccountContact.name("   "))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AccountContact.NAME_REQUIRED);
		assertThatThrownBy(() -> AccountContact.name("x".repeat(256)))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AccountContact.NAME_TOO_LONG);
	}

	@Test
	void contactEmailIsOptionalAndChecked() {
		assertThat(AccountContact.email("  ")).isNull();
		assertThat(AccountContact.email(" contact.parent@example.com ")).isEqualTo("contact.parent@example.com");
		assertThatThrownBy(() -> AccountContact.email("pas-un-mail"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AccountContact.EMAIL_INVALID);
	}

	@Test
	void phoneKeepsSpacesAndRefusesLetters() {
		assertThat(AccountContact.phone("+32 470 00 00 00")).isEqualTo("+32 470 00 00 00");
		assertThat(AccountContact.phone("0470000001")).isEqualTo("0470000001");
		assertThat(AccountContact.phone("")).isNull();
		assertThatThrownBy(() -> AccountContact.phone("abc"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AccountContact.PHONE_INVALID);
	}

	@Test
	void avatarIsOneOfTheSixOrEmpty() {
		assertThat(AccountContact.avatar(" owl ")).isEqualTo("owl");
		assertThat(AccountContact.avatar("")).isNull();
		assertThatThrownBy(() -> AccountContact.avatar("dragon"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(AccountContact.AVATAR_UNKNOWN);
		assertThat(AccountContact.CHOICES).extracting(choice -> choice.label())
				.containsExactly("Hibou", "Renard", "Ours", "Chat", "Panda", "Grenouille");
	}

	@Test
	void missingJsonValueKeepsTheColumn() {
		assertThat(AccountContact.merge(null, "deja@example.com", AccountContact::email))
				.isEqualTo("deja@example.com");
		assertThat(AccountContact.merge("", "deja@example.com", AccountContact::email)).isNull();
	}
}
