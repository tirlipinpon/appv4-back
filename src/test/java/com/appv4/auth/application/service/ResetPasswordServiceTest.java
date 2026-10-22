package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.PasswordResetTokenPort;
import com.appv4.auth.application.port.out.PasswordResetTokenPort.ValidResetToken;

@ExtendWith(MockitoExtension.class)
class ResetPasswordServiceTest {

	@Mock
	private PasswordResetTokenPort passwordResetTokenPort;

	@Mock
	private AuthUserAccountPort authUserAccountPort;

	@Mock
	private PasswordEncoder passwordEncoder;

	private ResetPasswordService service;

	@BeforeEach
	void setUp() {
		service = new ResetPasswordService(passwordResetTokenPort, authUserAccountPort, passwordEncoder);
	}

	@Test
	void weakPasswordRejectedWithoutTouchingUser() {
		assertThatThrownBy(() -> service.reset("clear-token", "123"))
				.isInstanceOf(AuthDomainException.class);

		verify(passwordResetTokenPort, never()).findValidByTokenHash(anyString());
		verify(authUserAccountPort, never()).updatePasswordHash(anyLong(), anyString());
	}

	@Test
	void unknownTokenRejected() {
		when(passwordResetTokenPort.findValidByTokenHash(anyString())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.reset("bad-token", "Jardin1!"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining(ResetPasswordService.INVALID_TOKEN_MESSAGE);

		verify(authUserAccountPort, never()).updatePasswordHash(anyLong(), anyString());
	}

	@Test
	void validTokenUpdatesPasswordAndConsumesToken() {
		String clear = "good-token";
		String hash = ForgotPasswordService.sha256Hex(clear);
		when(passwordResetTokenPort.findValidByTokenHash(hash))
				.thenReturn(Optional.of(new ValidResetToken(7L, 42L)));
		when(passwordEncoder.encode("Jardin1!")).thenReturn("bcrypt-hash");

		service.reset(clear, "Jardin1!");

		verify(authUserAccountPort).updatePasswordHash(42L, "bcrypt-hash");
		verify(passwordResetTokenPort).markUsed(7L);
		verify(passwordResetTokenPort).invalidateUnusedForUser(eq(42L));
	}
}
