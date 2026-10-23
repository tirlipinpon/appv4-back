package com.appv4.auth.application.service;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
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

import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.JwtTokenEncoder;
import com.appv4.auth.application.result.JwtTokenResult;

@ExtendWith(MockitoExtension.class)
class IssueJwtServiceTest {

	@Mock
	private JwtTokenEncoder jwtTokenEncoder;

	@Mock
	private ActiveRolePort activeRolePort;

	private IssueJwtService service;

	@BeforeEach
	void setUp() {
		service = new IssueJwtService(jwtTokenEncoder, activeRolePort);
		when(jwtTokenEncoder.encode(eq(1L), eq("a@b.com"), nullable(String.class), anyList(), anyBoolean()))
				.thenReturn(new JwtTokenResult("jwt", 10L));
	}

	@Test
	void singleRoleIsRememberedAndPutInTheToken() {
		when(activeRolePort.findSpaceRoles(1L)).thenReturn(List.of("parent"));
		when(activeRolePort.findLastActiveRole(1L)).thenReturn(Optional.empty());

		service.issue(1L, "a@b.com");

		verify(activeRolePort).saveLastActiveRole(1L, "parent");
		verify(jwtTokenEncoder).encode(1L, "a@b.com", "parent", List.of("parent"), false);
	}

	@Test
	void twoRolesReuseAStillValidMemory() {
		when(activeRolePort.findSpaceRoles(1L)).thenReturn(List.of("parent", "teacher"));
		when(activeRolePort.findLastActiveRole(1L)).thenReturn(Optional.of("teacher"));

		service.issue(1L, "a@b.com");

		verify(activeRolePort, never()).saveLastActiveRole(eq(1L), nullable(String.class));
		verify(jwtTokenEncoder).encode(1L, "a@b.com", "teacher", List.of("parent", "teacher"), false);
	}

	@Test
	void twoRolesWithoutMemoryDoNotClaimASpace() {
		when(activeRolePort.findSpaceRoles(1L)).thenReturn(List.of("parent", "teacher"));
		when(activeRolePort.findLastActiveRole(1L)).thenReturn(Optional.empty());

		service.issue(1L, "a@b.com");

		verify(activeRolePort, never()).saveLastActiveRole(eq(1L), nullable(String.class));
		verify(jwtTokenEncoder).encode(1L, "a@b.com", null, List.of("parent", "teacher"), false);
	}

	@Test
	void staleMemoryIsReplacedWhenOnlyOneRoleRemains() {
		when(activeRolePort.findSpaceRoles(1L)).thenReturn(List.of("parent"));
		when(activeRolePort.findLastActiveRole(1L)).thenReturn(Optional.of("teacher"));

		service.issue(1L, "a@b.com");

		verify(activeRolePort).saveLastActiveRole(1L, "parent");
		verify(jwtTokenEncoder).encode(1L, "a@b.com", "parent", List.of("parent"), false);
	}

	@Test
	void adminRoleIsAddedWithoutChoosingASpace() {
		when(activeRolePort.findSpaceRoles(1L)).thenReturn(List.of());
		when(activeRolePort.findLastActiveRole(1L)).thenReturn(Optional.empty());
		when(activeRolePort.hasAdminRole(1L)).thenReturn(true);

		service.issue(1L, "a@b.com");

		verify(activeRolePort, never()).saveLastActiveRole(eq(1L), nullable(String.class));
		verify(jwtTokenEncoder).encode(1L, "a@b.com", null, List.of("admin"), false);
	}

	@Test
	void memoryIsClearedWhenNoSpaceRoleRemains() {
		when(activeRolePort.findSpaceRoles(1L)).thenReturn(List.of());
		when(activeRolePort.findLastActiveRole(1L)).thenReturn(Optional.of("parent"));

		service.issue(1L, "a@b.com");

		verify(activeRolePort).saveLastActiveRole(1L, null);
		verify(jwtTokenEncoder).encode(1L, "a@b.com", null, List.of(), false);
	}
}
