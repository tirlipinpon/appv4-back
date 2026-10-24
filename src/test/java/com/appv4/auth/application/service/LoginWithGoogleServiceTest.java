package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.GoogleOAuthClient;
import com.appv4.auth.application.port.out.GoogleOAuthClient.GoogleUserProfile;
import com.appv4.auth.application.result.JwtTokenResult;

@ExtendWith(MockitoExtension.class)
class LoginWithGoogleServiceTest {

	@Mock
	private GoogleOAuthClient googleOAuthClient;

	@Mock
	private AuthUserAccountPort authUserAccountPort;

	@Mock
	private IssueJwtUseCase issueJwtUseCase;

	private LoginWithGoogleService service;

	@BeforeEach
	void setUp() {
		service = new LoginWithGoogleService(googleOAuthClient, authUserAccountPort, issueJwtUseCase);
	}

	@Test
	void firstLoginCreatesUserAndReturnsJwt() {
		when(googleOAuthClient.exchangeCode("code", "http://localhost/cb"))
				.thenReturn(new GoogleUserProfile("sub-1", "Marie@Example.com", true, "Marie"));
		when(authUserAccountPort.findByProviderSubject("google", "sub-1")).thenReturn(Optional.empty());
		when(authUserAccountPort.findByEmail("marie@example.com")).thenReturn(Optional.empty());
		when(authUserAccountPort.createWithIdentity(
				eq("marie@example.com"),
				eq("Marie"),
				eq("google"),
				eq("sub-1"),
				eq("marie@example.com")))
				.thenReturn(new AuthUserAccount(10L, "marie@example.com", "Marie", true));
		when(issueJwtUseCase.issue(10L, "marie@example.com"))
				.thenReturn(new JwtTokenResult("jwt-token", 123L));

		JwtTokenResult result = service.login("code", "http://localhost/cb");

		assertThat(result.token()).isEqualTo("jwt-token");
		verify(authUserAccountPort).createWithIdentity(
				"marie@example.com", "Marie", "google", "sub-1", "marie@example.com");
	}

	@Test
	void secondLoginReusesSameIdentity() {
		when(googleOAuthClient.exchangeCode("code", "http://localhost/cb"))
				.thenReturn(new GoogleUserProfile("sub-1", "marie@example.com", true, "Marie"));
		when(authUserAccountPort.findByProviderSubject("google", "sub-1"))
				.thenReturn(Optional.of(new AuthUserAccount(10L, "marie@example.com", "Marie", true)));
		when(issueJwtUseCase.issue(10L, "marie@example.com"))
				.thenReturn(new JwtTokenResult("jwt-token", 123L));

		service.login("code", "http://localhost/cb");

		verify(authUserAccountPort, never()).createWithIdentity(
				anyString(), anyString(), anyString(), anyString(), anyString());
		verify(authUserAccountPort, never()).linkIdentity(anyLong(), anyString(), anyString(), anyString());
	}

	@Test
	void unverifiedEmailIsRejected() {
		when(googleOAuthClient.exchangeCode("code", "http://localhost/cb"))
				.thenReturn(new GoogleUserProfile("sub-1", "x@y.com", false, "X"));

		assertThatThrownBy(() -> service.login("code", "http://localhost/cb"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("non vérifié");
	}

	@Test
	void linkingToExistingEmailClearsLocalPassword() {
		when(googleOAuthClient.exchangeCode("code", "http://localhost/cb"))
				.thenReturn(new GoogleUserProfile("sub-9", "victim@example.com", true, "Victim"));
		when(authUserAccountPort.findByProviderSubject("google", "sub-9")).thenReturn(Optional.empty());
		when(authUserAccountPort.findByEmail("victim@example.com"))
				.thenReturn(Optional.of(new AuthUserAccount(5L, "victim@example.com", "Victim", true)));
		when(issueJwtUseCase.issue(5L, "victim@example.com"))
				.thenReturn(new JwtTokenResult("jwt", 1L));

		service.login("code", "http://localhost/cb");

		verify(authUserAccountPort).linkIdentity(5L, "google", "sub-9", "victim@example.com");
		verify(authUserAccountPort).clearPasswordAfterVerifiedSocialLink(5L);
	}
}
