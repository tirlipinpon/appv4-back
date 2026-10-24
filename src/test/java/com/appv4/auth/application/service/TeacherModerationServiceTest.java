package com.appv4.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.TeacherApprovedMailPort;
import com.appv4.auth.application.port.out.TeacherModerationPort;
import com.appv4.auth.application.port.out.TeacherRequestPage;
import com.appv4.auth.application.port.out.TeacherRequestView;

@ExtendWith(MockitoExtension.class)
class TeacherModerationServiceTest {

	@Mock
	private ActiveRolePort activeRolePort;

	@Mock
	private TeacherModerationPort teacherModerationPort;

	@Mock
	private TeacherApprovedMailPort teacherApprovedMailPort;

	private TeacherModerationService service;

	@BeforeEach
	void setUp() {
		service = new TeacherModerationService(activeRolePort, teacherModerationPort, teacherApprovedMailPort);
	}

	@Test
	void parentCannotList() {
		when(activeRolePort.hasAdminRole(2L)).thenReturn(false);

		assertThatThrownBy(() -> service.list(2L, 50))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherModerationService.FORBIDDEN);
		verify(teacherModerationPort, never()).findModerationQueue(anyInt());
	}

	@Test
	void adminListsPendingAndApproved() {
		when(activeRolePort.hasAdminRole(1L)).thenReturn(true);
		TeacherRequestView pending = view(4L, "pending", null);
		when(teacherModerationPort.findModerationQueue(50))
				.thenReturn(new TeacherRequestPage(List.of(pending), false));

		TeacherRequestPage page = service.list(1L, 50);
		assertThat(page.items()).containsExactly(pending);
		assertThat(page.truncated()).isFalse();
	}

	@Test
	void listCapsLimitToMax() {
		when(activeRolePort.hasAdminRole(1L)).thenReturn(true);
		when(teacherModerationPort.findModerationQueue(TeacherModerationService.LIST_MAX_LIMIT))
				.thenReturn(new TeacherRequestPage(List.of(), false));

		service.list(1L, 10_000);
		verify(teacherModerationPort).findModerationQueue(TeacherModerationService.LIST_MAX_LIMIT);
	}

	@Test
	void approveGrantsTeacherAndSendsMailOnce() {
		when(activeRolePort.hasAdminRole(1L)).thenReturn(true);
		TeacherRequestView pending = view(4L, "pending", null);
		when(teacherModerationPort.findById(4L)).thenReturn(Optional.of(pending));

		TeacherRequestView approved = service.approve(1L, 4L);

		assertThat(approved.status()).isEqualTo("approved");
		assertThat(approved.reviewedAt()).isNotNull();
		verify(teacherModerationPort).markApproved(eq(4L), any(Instant.class));
		verify(teacherModerationPort).grantTeacher(9L);
		verify(teacherApprovedMailPort).sendTeacherApproved("prof@example.com");
	}

	@Test
	void secondApproveDoesNotSendMailAgain() {
		when(activeRolePort.hasAdminRole(1L)).thenReturn(true);
		TeacherRequestView already = view(4L, "approved", Instant.parse("2026-10-23T10:00:00Z"));
		when(teacherModerationPort.findById(4L)).thenReturn(Optional.of(already));

		TeacherRequestView result = service.approve(1L, 4L);

		assertThat(result.status()).isEqualTo("approved");
		assertThat(result.reviewedAt()).isAfter(already.reviewedAt());
		verify(teacherModerationPort).markApproved(eq(4L), any(Instant.class));
		verify(teacherModerationPort).grantTeacher(9L);
		verify(teacherApprovedMailPort, never()).sendTeacherApproved(any());
	}

	@Test
	void revokeRemovesTheTeacherRole() {
		when(activeRolePort.hasAdminRole(1L)).thenReturn(true);
		when(activeRolePort.findLastActiveRole(9L)).thenReturn(Optional.of("teacher"));
		when(activeRolePort.findSpaceRoles(9L)).thenReturn(List.of());
		TeacherRequestView approved = view(4L, "approved", Instant.parse("2026-10-23T10:00:00Z"));
		when(teacherModerationPort.findById(4L)).thenReturn(Optional.of(approved));

		TeacherRequestView revoked = service.revoke(1L, 4L);

		assertThat(revoked.status()).isEqualTo("revoked");
		assertThat(revoked.reviewedAt()).isEqualTo(approved.reviewedAt());
		assertThat(revoked.revokedAt()).isNotNull();
		verify(teacherModerationPort).markRevoked(eq(4L), any(Instant.class));
		verify(teacherModerationPort).revokeTeacher(9L);
		verify(activeRolePort).saveLastActiveRole(9L, null);
	}

	@Test
	void approveAgainAfterRevokeSendsMail() {
		when(activeRolePort.hasAdminRole(1L)).thenReturn(true);
		TeacherRequestView revoked = view(4L, "revoked", Instant.parse("2026-10-23T11:00:00Z"));
		when(teacherModerationPort.findById(4L)).thenReturn(Optional.of(revoked));

		TeacherRequestView approved = service.approve(1L, 4L);

		assertThat(approved.status()).isEqualTo("approved");
		verify(teacherModerationPort).grantTeacher(9L);
		verify(teacherApprovedMailPort).sendTeacherApproved("prof@example.com");
	}

	@Test
	void pendingCannotBeRevoked() {
		when(activeRolePort.hasAdminRole(1L)).thenReturn(true);
		when(teacherModerationPort.findById(4L)).thenReturn(Optional.of(view(4L, "pending", null)));

		assertThatThrownBy(() -> service.revoke(1L, 4L))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherModerationService.NOT_REVOKABLE);
	}

	@Test
	void unknownRequestIsNotFound() {
		when(activeRolePort.hasAdminRole(1L)).thenReturn(true);
		when(teacherModerationPort.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.approve(1L, 99L))
				.isInstanceOf(AuthDomainException.class)
				.hasMessage(TeacherModerationService.NOT_FOUND);
	}

	private static TeacherRequestView view(long id, String status, Instant reviewedAt) {
		return new TeacherRequestView(
				id,
				9L,
				"prof@example.com",
				"Prof Test",
				status,
				Instant.parse("2026-10-23T09:00:00Z"),
				reviewedAt,
				null);
	}
}
