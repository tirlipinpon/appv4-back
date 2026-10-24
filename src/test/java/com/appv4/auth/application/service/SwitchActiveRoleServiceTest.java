package com.appv4.auth.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.IssueJwtUseCase;
import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.result.JwtTokenResult;

@ExtendWith(MockitoExtension.class)
class SwitchActiveRoleServiceTest {

	@Mock
	private ActiveRolePort activeRolePort;

	@Mock
	private IssueJwtUseCase issueJwtUseCase;

	private SwitchActiveRoleService service;

	@BeforeEach
	void setUp() {
		service = new SwitchActiveRoleService(activeRolePort, issueJwtUseCase);
	}

	@Test
	void switchRemembersTheRoleThenReissuesTheToken() {
		when(activeRolePort.findSpaceRoles(4L)).thenReturn(List.of("parent", "teacher"));
		when(issueJwtUseCase.issue(4L, "a@b.com")).thenReturn(new JwtTokenResult("jwt", 9L));

		JwtTokenResult result = service.switchRole(4L, "a@b.com", "TEACHER");

		assertEquals("jwt", result.token());
		InOrder order = inOrder(activeRolePort, issueJwtUseCase);
		order.verify(activeRolePort).saveLastActiveRole(4L, "teacher");
		order.verify(issueJwtUseCase).issue(4L, "a@b.com");
	}

	@Test
	void switchToARoleTheUserDoesNotHaveIsForbiddenAndDoesNotWrite() {
		when(activeRolePort.findSpaceRoles(4L)).thenReturn(List.of("parent"));

		AuthDomainException ex = assertThrows(
				AuthDomainException.class,
				() -> service.switchRole(4L, "a@b.com", "teacher"));

		assertEquals(com.appv4.auth.application.exception.AuthErrorCode.FORBIDDEN, ex.getErrorCode());
		assertEquals(SwitchActiveRoleService.ROLE_NOT_GRANTED, ex.getMessage());
		verify(activeRolePort, never()).saveLastActiveRole(anyLong(), any());
		verifyNoInteractions(issueJwtUseCase);
	}

	@Test
	void adminIsNotASwitchableSpace() {
		AuthDomainException ex = assertThrows(
				AuthDomainException.class,
				() -> service.switchRole(4L, "a@b.com", "admin"));

		assertEquals(com.appv4.auth.application.exception.AuthErrorCode.FORBIDDEN, ex.getErrorCode());
		verifyNoInteractions(activeRolePort);
		verifyNoInteractions(issueJwtUseCase);
	}
}
