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
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.FacebookOAuthClient;
import com.appv4.auth.application.port.out.FacebookOAuthClient.FacebookUserProfile;
import com.appv4.auth.application.result.JwtTokenResult;

@ExtendWith(MockitoExtension.class)
class LoginWithFacebookServiceTest {

	@Mock
	private FacebookOAuthClient facebookOAuthClient;

	@Mock
	private AuthUserAccountPort authUserAccountPort;

	@Mock
	private IssueJwtUseCase issueJwtUseCase;

	private LoginWithFacebookService service;

	@BeforeEach
	void setUp() {
		service = new LoginWithFacebookService(facebookOAuthClient, authUserAccountPort, issueJwtUseCase);
	}

	@Test
	void firstLoginCreatesUserAndReturnsJwt() {
		when(facebookOAuthClient.exchangeCode("code", "http://localhost/cb"))
				.thenReturn(new FacebookUserProfile("fb-1", "Marie@Example.com", "Marie"));
		when(authUserAccountPort.findByProviderSubject("facebook", "fb-1")).thenReturn(Optional.empty());
		when(authUserAccountPort.findByEmail("marie@example.com")).thenReturn(Optional.empty());
		when(authUserAccountPort.createWithIdentity(
				eq("marie@example.com"),
				eq("Marie"),
				eq("facebook"),
				eq("fb-1"),
				eq("marie@example.com")))
				.thenReturn(new AuthUserAccount(10L, "marie@example.com", "Marie", true));
		when(issueJwtUseCase.issue(10L, "marie@example.com"))
				.thenReturn(new JwtTokenResult("jwt-token", 123L));

		JwtTokenResult result = service.login("code", "http://localhost/cb");

		assertThat(result.token()).isEqualTo("jwt-token");
	}

	@Test
	void missingEmailIsRejected() {
		when(facebookOAuthClient.exchangeCode("code", "http://localhost/cb"))
				.thenReturn(new FacebookUserProfile("fb-1", null, "X"));

		assertThatThrownBy(() -> service.login("code", "http://localhost/cb"))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("Email Facebook manquant");
		verify(authUserAccountPort, never()).createWithIdentity(
				anyString(), anyString(), anyString(), anyString(), anyString());
	}
}
