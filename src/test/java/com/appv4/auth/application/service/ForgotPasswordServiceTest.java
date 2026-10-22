package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.PasswordResetMailPort;
import com.appv4.auth.application.port.out.PasswordResetTokenPort;
import com.appv4.auth.infrastructure.config.PasswordResetProperties;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceTest {

	@Mock
	private AuthUserAccountPort authUserAccountPort;

	@Mock
	private PasswordResetTokenPort passwordResetTokenPort;

	@Mock
	private PasswordResetMailPort passwordResetMailPort;

	private ForgotPasswordService service;

	@BeforeEach
	void setUp() {
		PasswordResetProperties props = new PasswordResetProperties();
		props.setTokenTtl(Duration.ofMinutes(45));
		props.setFrontendResetUrl("http://localhost:4201/reset-password");
		service = new ForgotPasswordService(
				authUserAccountPort,
				passwordResetTokenPort,
				passwordResetMailPort,
				props);
	}

	@Test
	void knownEmailCreatesTokenAndSendsMail() {
		when(authUserAccountPort.findByEmail("marie@example.com"))
				.thenReturn(Optional.of(new AuthUserAccount(10L, "marie@example.com", "Marie", true)));

		String message = service.requestReset("Marie@Example.com");

		assertThat(message).isEqualTo(ForgotPasswordService.PUBLIC_MESSAGE);
		verify(passwordResetTokenPort).invalidateUnusedForUser(10L);
		ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Instant> expires = ArgumentCaptor.forClass(Instant.class);
		verify(passwordResetTokenPort).create(eq(10L), hash.capture(), expires.capture());
		assertThat(hash.getValue()).hasSize(64);
		assertThat(expires.getValue()).isAfter(Instant.now().plus(Duration.ofMinutes(40)));

		ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
		verify(passwordResetMailPort).sendResetLink(eq("marie@example.com"), url.capture());
		assertThat(url.getValue()).startsWith("http://localhost:4201/reset-password?token=");
	}

	@Test
	void unknownEmailReturnsSameMessageWithoutSideEffects() {
		when(authUserAccountPort.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		String message = service.requestReset("nobody@example.com");

		assertThat(message).isEqualTo(ForgotPasswordService.PUBLIC_MESSAGE);
		verify(passwordResetTokenPort, never()).invalidateUnusedForUser(anyLong());
		verify(passwordResetTokenPort, never()).create(anyLong(), anyString(), any());
		verify(passwordResetMailPort, never()).sendResetLink(anyString(), anyString());
	}
}
