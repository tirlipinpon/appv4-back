package com.appv4.auth.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtPropertiesGuardTest {

	@Test
	void localMayUseDemoSecret() {
		assertThatCode(() -> JwtPropertiesGuard.validateSecret(
				JwtPropertiesGuard.LOCAL_DEMO_SECRET, true))
				.doesNotThrowAnyException();
	}

	@Test
	void nonLocalRejectsDemoSecret() {
		assertThatThrownBy(() -> JwtPropertiesGuard.validateSecret(
				JwtPropertiesGuard.LOCAL_DEMO_SECRET, false))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("démonstration");
	}

	@Test
	void nonLocalRejectsBlankSecret() {
		assertThatThrownBy(() -> JwtPropertiesGuard.validateSecret("  ", false))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("manquant");
	}

	@Test
	void rejectsShortSecret() {
		assertThatThrownBy(() -> JwtPropertiesGuard.validateSecret("too-short", true))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("trop court");
	}

	@Test
	void acceptsValidExpirationBounds() {
		assertThatCode(() -> JwtPropertiesGuard.validateExpiration(1)).doesNotThrowAnyException();
		assertThatCode(() -> JwtPropertiesGuard.validateExpiration(10_080)).doesNotThrowAnyException();
	}

	@Test
	void rejectsInvalidExpiration() {
		assertThatThrownBy(() -> JwtPropertiesGuard.validateExpiration(0))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> JwtPropertiesGuard.validateExpiration(10_081))
				.isInstanceOf(IllegalStateException.class);
	}
}
