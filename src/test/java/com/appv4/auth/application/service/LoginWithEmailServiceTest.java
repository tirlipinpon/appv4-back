package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserLoginCandidate;
import com.appv4.auth.application.result.JwtTokenResult;

@ExtendWith(MockitoExtension.class)
class LoginWithEmailServiceTest {

	@Mock
	private AuthUserAccountPort authUserAccountPort;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private IssueJwtUseCase issueJwtUseCase;

	private LoginWithEmailService service;

	@BeforeEach
	void setUp() {
		service = new LoginWithEmailService(authUserAccountPort, passwordEncoder, issueJwtUseCase);
	}

	@Test
	void loginReturnsJwtWhenCredentialsMatch() {
		when(authUserAccountPort.findLoginCandidateByEmail("marie@example.com"))
				.thenReturn(Optional.of(new AuthUserLoginCandidate(
						10L, "marie@example.com", "bcrypt-hash", true)));
		when(passwordEncoder.matches("Secret1!", "bcrypt-hash")).thenReturn(true);
		when(issueJwtUseCase.issue(10L, "marie@example.com"))
				.thenReturn(new JwtTokenResult("jwt-token", 123L));

		JwtTokenResult result = service.login("Marie@Example.com", "Secret1!");

		assertThat(result.token()).isEqualTo("jwt-token");
		verify(issueJwtUseCase).issue(10L, "marie@example.com");
	}

	@Test
	void unknownEmailReturnsSameUnauthorizedMessage() {
		when(authUserAccountPort.findLoginCandidateByEmail("ghost@example.com"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.login("ghost@example.com", "Secret1!"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LoginWithEmailService.INVALID_CREDENTIALS_MESSAGE)
				.extracting("httpStatus")
				.isEqualTo(401);
		verify(issueJwtUseCase, never()).issue(anyLong(), anyString());
	}

	@Test
	void wrongPasswordReturnsSameUnauthorizedMessage() {
		when(authUserAccountPort.findLoginCandidateByEmail("marie@example.com"))
				.thenReturn(Optional.of(new AuthUserLoginCandidate(
						10L, "marie@example.com", "bcrypt-hash", true)));
		when(passwordEncoder.matches("Wrong1!", "bcrypt-hash")).thenReturn(false);

		assertThatThrownBy(() -> service.login("marie@example.com", "Wrong1!"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LoginWithEmailService.INVALID_CREDENTIALS_MESSAGE)
				.extracting("httpStatus")
				.isEqualTo(401);
		verify(issueJwtUseCase, never()).issue(anyLong(), anyString());
	}

	@Test
	void socialOnlyAccountWithoutHashReturnsUnauthorized() {
		when(authUserAccountPort.findLoginCandidateByEmail("social@example.com"))
				.thenReturn(Optional.of(new AuthUserLoginCandidate(
						7L, "social@example.com", null, true)));

		assertThatThrownBy(() -> service.login("social@example.com", "Secret1!"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LoginWithEmailService.INVALID_CREDENTIALS_MESSAGE)
				.extracting("httpStatus")
				.isEqualTo(401);
	}

	@Test
	void blankPasswordReturnsUnauthorized() {
		assertThatThrownBy(() -> service.login("marie@example.com", "  "))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LoginWithEmailService.INVALID_CREDENTIALS_MESSAGE)
				.extracting("httpStatus")
				.isEqualTo(401);
		verify(authUserAccountPort, never()).findLoginCandidateByEmail(anyString());
	}
}
