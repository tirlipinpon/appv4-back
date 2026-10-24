package com.appv4.auth.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class MailTransportGuardTest {

	@Test
	void loggingAllowedOnLocal() {
		MailTransportProperties props = new MailTransportProperties();
		props.setTransport("logging");
		Environment env = mock(Environment.class);
		when(env.getActiveProfiles()).thenReturn(new String[] {"local"});

		assertThatCode(() -> new MailTransportGuard(props, env).afterPropertiesSet())
				.doesNotThrowAnyException();
	}

	@Test
	void loggingForbiddenOutsideLocalAndTest() {
		MailTransportProperties props = new MailTransportProperties();
		props.setTransport("logging");
		Environment env = mock(Environment.class);
		when(env.getActiveProfiles()).thenReturn(new String[0]);

		assertThatThrownBy(() -> new MailTransportGuard(props, env).afterPropertiesSet())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("logging");
	}

	@Test
	void noopAllowedOutsideLocal() {
		MailTransportProperties props = new MailTransportProperties();
		props.setTransport("noop");
		Environment env = mock(Environment.class);
		when(env.getActiveProfiles()).thenReturn(new String[0]);

		assertThatCode(() -> new MailTransportGuard(props, env).afterPropertiesSet())
				.doesNotThrowAnyException();
	}
}
