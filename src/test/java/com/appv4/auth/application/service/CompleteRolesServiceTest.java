package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.out.SignupRolePort;
import com.appv4.auth.application.result.JwtTokenResult;

@ExtendWith(MockitoExtension.class)
class CompleteRolesServiceTest {

	@Mock
	private SignupRolePort signupRolePort;

	@Mock
	private IssueJwtUseCase issueJwtUseCase;

	private CompleteRolesService service;

	@BeforeEach
	void setUp() {
		service = new CompleteRolesService(signupRolePort, issueJwtUseCase);
		lenient().when(issueJwtUseCase.issue(4L, "a@b.com")).thenReturn(new JwtTokenResult("jwt", 1L));
	}

	@Test
	void parentIsGrantedImmediately() {
		when(signupRolePort.hasCompletedInitialChoice(4L)).thenReturn(false);
		when(signupRolePort.tryClaimInitialChoice(4L)).thenReturn(true);

		service.complete(4L, "a@b.com", List.of("parent"));

		verify(signupRolePort).grantParent(4L);
		verify(signupRolePort, never()).requestTeacher(4L);
	}

	@Test
	void teacherIsOnlyAPendingRequest() {
		when(signupRolePort.hasCompletedInitialChoice(4L)).thenReturn(false);
		when(signupRolePort.tryClaimInitialChoice(4L)).thenReturn(true);

		service.complete(4L, "a@b.com", List.of("teacher"));

		verify(signupRolePort).requestTeacher(4L);
		verify(signupRolePort, never()).grantParent(4L);
	}

	@Test
	void bothWritesParentAndTeacherRequest() {
		when(signupRolePort.hasCompletedInitialChoice(4L)).thenReturn(false);
		when(signupRolePort.tryClaimInitialChoice(4L)).thenReturn(true);

		service.complete(4L, "a@b.com", List.of("parent", "TEACHER"));

		verify(signupRolePort).grantParent(4L);
		verify(signupRolePort).requestTeacher(4L);
	}

	@Test
	void secondCallDoesNotRewriteAnExistingChoice() {
		when(signupRolePort.hasCompletedInitialChoice(4L)).thenReturn(true);

		service.complete(4L, "a@b.com", List.of("teacher"));

		verify(signupRolePort, never()).tryClaimInitialChoice(4L);
		verify(signupRolePort, never()).grantParent(4L);
		verify(signupRolePort, never()).requestTeacher(4L);
	}

	@Test
	void concurrentLoserDoesNotWriteRoles() {
		when(signupRolePort.hasCompletedInitialChoice(4L)).thenReturn(false);
		when(signupRolePort.tryClaimInitialChoice(4L)).thenReturn(false);

		assertThat(service.complete(4L, "a@b.com", List.of("parent")).token()).isEqualTo("jwt");

		verify(signupRolePort, never()).grantParent(4L);
		verify(signupRolePort, never()).requestTeacher(4L);
	}

	@Test
	void revokedTeacherWithoutSpacesStillBlocksNewChoice() {
		when(signupRolePort.hasCompletedInitialChoice(4L)).thenReturn(true);

		service.complete(4L, "a@b.com", List.of("parent"));

		verify(signupRolePort, never()).grantParent(4L);
		verify(signupRolePort, never()).requestTeacher(4L);
	}

	@Test
	void rejectsEmptyOrAdminBeforeClaiming() {
		when(signupRolePort.hasCompletedInitialChoice(4L)).thenReturn(false);

		assertThatThrownBy(() -> service.complete(4L, "a@b.com", List.of()))
				.isInstanceOf(AuthDomainException.class);
		assertThatThrownBy(() -> service.complete(4L, "a@b.com", List.of("admin")))
				.isInstanceOf(AuthDomainException.class)
				.hasMessageContaining("non autorisé");
		verify(signupRolePort, never()).tryClaimInitialChoice(4L);
		verify(signupRolePort, never()).grantParent(4L);
		verify(signupRolePort, never()).requestTeacher(4L);
	}

	@Test
	void returnsTheToken() {
		when(signupRolePort.hasCompletedInitialChoice(4L)).thenReturn(false);
		when(signupRolePort.tryClaimInitialChoice(4L)).thenReturn(true);

		assertThat(service.complete(4L, "a@b.com", List.of("parent")).token()).isEqualTo("jwt");
	}
}
