package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.result.ChildLoginResult;
import com.appv4.auth.application.result.JwtTokenResult;

@ExtendWith(MockitoExtension.class)
class LoginChildServiceTest {

	@Mock
	private ChildLoginPort childLoginPort;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ChildSessionTokenPort childSessionTokenPort;

	private LoginChildService service;

	@BeforeEach
	void setUp() {
		service = new LoginChildService(childLoginPort, passwordEncoder, childSessionTokenPort);
	}

	@Test
	void loginReturnsChildTokenWhenOnePinMatches() {
		when(childLoginPort.findActiveByFirstName("léa"))
				.thenReturn(List.of(new ChildLoginCandidate(7L, "Léa", "hash")));
		when(passwordEncoder.matches("1234", "hash")).thenReturn(true);
		when(childSessionTokenPort.issue(7L, "Léa")).thenReturn(new JwtTokenResult("child-jwt", 99L));

		ChildLoginResult result = service.login("  Léa  ", "1234");

		assertThat(result.token()).isEqualTo("child-jwt");
		assertThat(result.firstName()).isEqualTo("Léa");
		assertThat(result.expiresAtEpochSeconds()).isEqualTo(99L);
	}

	@Test
	void unknownNameUsesDummyHashAndSameMessage() {
		when(childLoginPort.findActiveByFirstName("ghost")).thenReturn(List.of());
		when(passwordEncoder.matches("1234", LoginWithEmailService.DUMMY_BCRYPT_HASH)).thenReturn(false);

		assertThatThrownBy(() -> service.login("Ghost", "1234"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LoginChildService.INVALID_CREDENTIALS_MESSAGE)
				.extracting("errorCode")
				.isEqualTo(AuthErrorCode.UNAUTHORIZED);
		verify(childSessionTokenPort, never()).issue(anyLong(), anyString());
	}

	@Test
	void wrongPinReturnsSameMessage() {
		when(childLoginPort.findActiveByFirstName("léa"))
				.thenReturn(List.of(new ChildLoginCandidate(7L, "Léa", "hash")));
		when(passwordEncoder.matches("0000", "hash")).thenReturn(false);

		assertThatThrownBy(() -> service.login("Léa", "0000"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LoginChildService.INVALID_CREDENTIALS_MESSAGE);
		verify(childSessionTokenPort, never()).issue(anyLong(), anyString());
	}

	@Test
	void twoMatchingChildrenAreRefusedWithTheSameMessage() {
		when(childLoginPort.findActiveByFirstName("léa")).thenReturn(List.of(
				new ChildLoginCandidate(7L, "Léa", "hash-a"),
				new ChildLoginCandidate(8L, "Léa", "hash-b")));
		when(passwordEncoder.matches("1234", "hash-a")).thenReturn(true);
		when(passwordEncoder.matches("1234", "hash-b")).thenReturn(true);

		assertThatThrownBy(() -> service.login("Léa", "1234"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LoginChildService.INVALID_CREDENTIALS_MESSAGE);
		verify(childSessionTokenPort, never()).issue(anyLong(), anyString());
	}

	@Test
	void shortPinDoesNotLookUpChildren() {
		when(passwordEncoder.matches("12", LoginWithEmailService.DUMMY_BCRYPT_HASH)).thenReturn(false);

		assertThatThrownBy(() -> service.login("Léa", "12"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LoginChildService.INVALID_CREDENTIALS_MESSAGE);
		verify(childLoginPort, never()).findActiveByFirstName(anyString());
	}

	@Test
	void sessionRefusesWhenTheChildIsNoLongerActive() {
		when(childSessionTokenPort.read("token"))
				.thenReturn(Optional.of(new ChildSessionTokenPort.ChildSession(7L)));
		when(childLoginPort.findActiveById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.session("token"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(LoginChildService.INVALID_SESSION_MESSAGE);
	}
}
