package com.appv4.auth.infrastructure.adapter.out.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.appv4.auth.application.port.out.JwtTokenDecoder;
import com.appv4.auth.application.result.JwtTokenResult;
import com.appv4.auth.infrastructure.config.JwtProperties;

class JjwtTokenAdapterTest {

	private JjwtTokenAdapter adapter;

	@BeforeEach
	void setUp() {
		JwtProperties properties = new JwtProperties();
		properties.setSecret("appv4-local-dev-jwt-secret-change-me-32b");
		properties.setIssuer("appv4-back-test");
		properties.setExpirationMinutes(60);
		adapter = new JjwtTokenAdapter(properties);
	}

	@Test
	void encodeThenDecodeReturnsSameUser() {
		JwtTokenResult issued = adapter.encode(42L, "marie@example.com", null);

		assertTrue(issued.token() != null && !issued.token().isBlank());
		assertTrue(issued.expiresAtEpochSeconds() > 0);

		var decoded = adapter.decode(issued.token());
		assertTrue(decoded.isPresent());
		JwtTokenDecoder.AuthenticatedUser user = decoded.get();
		assertEquals(42L, user.userId());
		assertEquals("marie@example.com", user.email());
		assertNull(user.activeRole());
	}

	@Test
	void encodePutsActiveRoleClaimWhenSpaceIsChosen() {
		JwtTokenResult issued = adapter.encode(7L, "prof@example.com", "teacher");

		var decoded = adapter.decode(issued.token());
		assertTrue(decoded.isPresent());
		assertEquals("teacher", decoded.get().activeRole());
	}

	@Test
	void decodeRejectsTamperedToken() {
		JwtTokenResult issued = adapter.encode(1L, "a@b.com", "parent");
		String tampered = issued.token() + "x";

		assertTrue(adapter.decode(tampered).isEmpty());
	}
}
