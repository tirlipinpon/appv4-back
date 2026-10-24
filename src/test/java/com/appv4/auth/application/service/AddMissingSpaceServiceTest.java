package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.SignupRolePort;
import com.appv4.auth.application.result.JwtTokenResult;
import com.appv4.auth.application.result.SpaceAccess;

@ExtendWith(MockitoExtension.class)
class AddMissingSpaceServiceTest {

	@Mock
	private SignupRolePort signupRolePort;

	@Mock
	private ActiveRolePort activeRolePort;

	@Mock
	private IssueJwtUseCase issueJwtUseCase;

	private AddMissingSpaceService service;

	@BeforeEach
	void setUp() {
		service = new AddMissingSpaceService(signupRolePort, activeRolePort, issueJwtUseCase);
	}

	@Test
	void openParentGrantsTheRoleAndIssuesAToken() {
		when(issueJwtUseCase.issue(4L, "a@b.com")).thenReturn(new JwtTokenResult("jwt-parent", 9L));

		JwtTokenResult result = service.openParent(4L, "a@b.com");

		verify(signupRolePort).grantParent(4L);
		verify(signupRolePort, never()).requestTeacher(4L);
		assertThat(result.token()).isEqualTo("jwt-parent");
	}

	@Test
	void requestTeacherOpensAPendingDemandWithoutAToken() {
		when(activeRolePort.findSpaceRoles(4L)).thenReturn(List.of("parent"));
		when(activeRolePort.teacherRequestStatus(4L)).thenReturn("pending");

		SpaceAccess access = service.requestTeacher(4L);

		verify(signupRolePort).requestTeacher(4L);
		verify(issueJwtUseCase, never()).issue(4L, "a@b.com");
		assertThat(access.roles()).containsExactly("parent");
		assertThat(access.teacherRequest()).isEqualTo("pending");
	}

	@Test
	void requestTeacherDoesNothingWhenTheRoleIsAlreadyGranted() {
		when(activeRolePort.findSpaceRoles(4L)).thenReturn(List.of("parent", "teacher"));
		when(activeRolePort.teacherRequestStatus(4L)).thenReturn("approved");

		SpaceAccess access = service.requestTeacher(4L);

		verify(signupRolePort, never()).requestTeacher(4L);
		assertThat(access.roles()).contains("teacher");
		assertThat(access.teacherRequest()).isEqualTo("approved");
	}

	@Test
	void viewReadsRolesAndRequestWithoutWriting() {
		when(activeRolePort.findSpaceRoles(4L)).thenReturn(List.of("parent"));
		when(activeRolePort.teacherRequestStatus(4L)).thenReturn("none");

		SpaceAccess access = service.view(4L);

		verify(signupRolePort, never()).grantParent(4L);
		verify(signupRolePort, never()).requestTeacher(4L);
		assertThat(access.roles()).containsExactly("parent");
		assertThat(access.teacherRequest()).isEqualTo("none");
	}
}
