package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.SignupRolePort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserSignupState;
import com.appv4.auth.application.result.JwtTokenResult;

@ExtendWith(MockitoExtension.class)
class SignupWithEmailServiceTest {

	@Mock
	private AuthUserAccountPort authUserAccountPort;

	@Mock
	private SignupRolePort signupRolePort;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private IssueJwtUseCase issueJwtUseCase;

	private SignupWithEmailService service;

	@BeforeEach
	void setUp() {
		service = new SignupWithEmailService(
				authUserAccountPort, signupRolePort, passwordEncoder, issueJwtUseCase);
	}

	@Test
	void createsAccountAndReturnsJwt() {
		when(authUserAccountPort.findSignupStateByEmail("marie@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("Secret1!")).thenReturn("bcrypt-hash");
		when(authUserAccountPort.createWithPassword(
				eq("marie@example.com"), eq("Marie"), eq("bcrypt-hash")))
				.thenReturn(new AuthUserAccount(10L, "marie@example.com", "Marie", true));
		when(issueJwtUseCase.issue(10L, "marie@example.com"))
				.thenReturn(new JwtTokenResult("jwt-token", 123L));

		JwtTokenResult result = service.signup(
				"Marie@Example.com", "Secret1!", "Marie", List.of("parent"));

		assertThat(result.token()).isEqualTo("jwt-token");
		verify(authUserAccountPort).createWithPassword("marie@example.com", "Marie", "bcrypt-hash");
		verify(signupRolePort).grantParent(10L);
		verify(signupRolePort, never()).requestTeacher(10L);
	}

	@Test
	void derivesDisplayNameFromEmailWhenBlank() {
		when(authUserAccountPort.findSignupStateByEmail("marie@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("Secret1!")).thenReturn("hash");
		when(authUserAccountPort.createWithPassword(anyString(), anyString(), anyString()))
				.thenReturn(new AuthUserAccount(1L, "marie@example.com", "marie", true));
		when(issueJwtUseCase.issue(1L, "marie@example.com"))
				.thenReturn(new JwtTokenResult("jwt", 1L));

		service.signup("marie@example.com", "Secret1!", "  ", List.of("parent"));

		ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
		verify(authUserAccountPort).createWithPassword(eq("marie@example.com"), name.capture(), eq("hash"));
		assertThat(name.getValue()).isEqualTo("marie");
	}

	@Test
	void rejectsWeakPassword() {
		assertThatThrownBy(() -> service.signup("a@b.com", "short", "A", List.of("parent")))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("trop court");
		verify(authUserAccountPort, never()).createWithPassword(anyString(), anyString(), anyString());
	}

	@Test
	void rejectsPasswordWithoutComplexity() {
		assertThatThrownBy(() -> service.signup("a@b.com", "alllowercase1!", "A", List.of("parent")))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("majuscule");
	}

	@Test
	void rejectsWhenPasswordAccountAlreadyExists() {
		when(authUserAccountPort.findSignupStateByEmail("marie@example.com"))
				.thenReturn(Optional.of(new AuthUserSignupState(
						5L, "marie@example.com", true, List.of())));

		assertThatThrownBy(() -> service.signup("marie@example.com", "Secret1!", "Marie", List.of("parent")))
				.isInstanceOf(AuthDomainException.class)
				.extracting("httpStatus")
				.isEqualTo(409);
		verify(authUserAccountPort, never()).createWithPassword(anyString(), anyString(), anyString());
	}

	@Test
	void rejectsWhenSocialAccountExistsWithGuidance() {
		when(authUserAccountPort.findSignupStateByEmail("marie@example.com"))
				.thenReturn(Optional.of(new AuthUserSignupState(
						5L, "marie@example.com", false, List.of("google", "facebook"))));

		assertThatThrownBy(() -> service.signup("marie@example.com", "Secret1!", "Marie", List.of("parent")))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("Google")
				.hasMessageContaining("Facebook")
				.hasMessageContaining("Mot de passe oublié");
	}

	@Test
	void teacherRequestDoesNotGrantTeacherRole() {
		when(authUserAccountPort.findSignupStateByEmail("prof@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("Secret1!")).thenReturn("hash");
		when(authUserAccountPort.createWithPassword(anyString(), anyString(), anyString()))
				.thenReturn(new AuthUserAccount(7L, "prof@example.com", "Prof", true));
		when(issueJwtUseCase.issue(7L, "prof@example.com")).thenReturn(new JwtTokenResult("jwt", 1L));

		service.signup("prof@example.com", "Secret1!", "Prof", List.of("teacher"));

		verify(signupRolePort).requestTeacher(7L);
		verify(signupRolePort, never()).grantParent(7L);
	}

	@Test
	void parentAndTeacherWritesBothWithoutTeacherRole() {
		when(authUserAccountPort.findSignupStateByEmail("both@example.com")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("Secret1!")).thenReturn("hash");
		when(authUserAccountPort.createWithPassword(anyString(), anyString(), anyString()))
				.thenReturn(new AuthUserAccount(8L, "both@example.com", "Both", true));
		when(issueJwtUseCase.issue(8L, "both@example.com")).thenReturn(new JwtTokenResult("jwt", 1L));

		service.signup("both@example.com", "Secret1!", "Both", List.of("parent", "TEACHER"));

		verify(signupRolePort).grantParent(8L);
		verify(signupRolePort).requestTeacher(8L);
	}

	@Test
	void rejectsMissingOrAdminRoleBeforeCreatingAccount() {
		assertThatThrownBy(() -> service.signup("a@b.com", "Secret1!", "A", List.of()))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("Parent");
		assertThatThrownBy(() -> service.signup("a@b.com", "Secret1!", "A", List.of("admin")))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("non autorisé");
		verifyNoInteractions(authUserAccountPort);
	}
}
