package com.appv4.auth.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;

class CurrentAuthTest {

	@Test
	void requireReturnsSameUser() {
		AuthenticatedUser user = new AuthenticatedUser(
				1L, "a@b.com", "parent", List.of("parent"), false, 0);
		assertThat(CurrentAuth.require(user)).isSameAs(user);
	}

	@Test
	void requireRejectsNull() {
		assertThatThrownBy(() -> CurrentAuth.require(null))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(CurrentAuth.REQUIRED);
	}
}
