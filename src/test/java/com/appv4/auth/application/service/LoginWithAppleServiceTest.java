package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.out.AppleOAuthClient;
import com.appv4.auth.application.port.out.AppleOAuthClient.AppleUserProfile;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.result.JwtTokenResult;

@ExtendWith(MockitoExtension.class)
class LoginWithAppleServiceTest {

	@Mock
	private AppleOAuthClient appleOAuthClient;

	@Mock
	private AuthUserAccountPort authUserAccountPort;

	@Mock
	private IssueJwtUseCase issueJwtUseCase;

	private LoginWithAppleService service;

	@BeforeEach
	void setUp() {
		service = new LoginWithAppleService(appleOAuthClient, authUserAccountPort, issueJwtUseCase);
	}

	@Test
	void firstLoginCreatesUserAndReturnsJwt() {
		when(appleOAuthClient.exchangeCode("code", "http://localhost/cb"))
				.thenReturn(new AppleUserProfile("apple-1", "Marie@Example.com", ""));
		when(authUserAccountPort.findByProviderSubject("apple", "apple-1")).thenReturn(Optional.empty());
		when(authUserAccountPort.findByEmail("marie@example.com")).thenReturn(Optional.empty());
		when(authUserAccountPort.createWithIdentity(
				eq("marie@example.com"),
				eq("marie"),
				eq("apple"),
				eq("apple-1"),
				eq("marie@example.com")))
				.thenReturn(new AuthUserAccount(10L, "marie@example.com", "marie", true));
		when(issueJwtUseCase.issue(10L, "marie@example.com"))
				.thenReturn(new JwtTokenResult("jwt-token", 123L));

		JwtTokenResult result = service.login("code", "http://localhost/cb");

		assertThat(result.token()).isEqualTo("jwt-token");
	}

	@Test
	void subsequentLoginWithoutEmailUsesProviderSubject() {
		when(appleOAuthClient.exchangeCode("code", "http://localhost/cb"))
				.thenReturn(new AppleUserProfile("apple-1", null, ""));
		when(authUserAccountPort.findByProviderSubject("apple", "apple-1"))
				.thenReturn(Optional.of(new AuthUserAccount(7L, "user@example.com", "User", true)));
		when(issueJwtUseCase.issue(7L, "user@example.com"))
				.thenReturn(new JwtTokenResult("jwt", 1L));

		JwtTokenResult result = service.login("code", "http://localhost/cb");

		assertThat(result.token()).isEqualTo("jwt");
		verify(authUserAccountPort, never()).createWithIdentity(
				anyString(), anyString(), anyString(), anyString(), anyString());
	}

	@Test
	void unknownSubjectWithoutEmailIsRejected() {
		when(appleOAuthClient.exchangeCode("code", "http://localhost/cb"))
				.thenReturn(new AppleUserProfile("apple-new", null, ""));
		when(authUserAccountPort.findByProviderSubject("apple", "apple-new")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.login("code", "http://localhost/cb"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("Email Apple manquant");
	}
}
