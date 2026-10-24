package com.appv4.auth.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.SessionState;
import com.appv4.auth.application.port.out.JwtTokenDecoder;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

	@Mock
	private JwtTokenDecoder jwtTokenDecoder;

	@Mock
	private AuthUserAccountPort authUserAccountPort;

	@Mock
	private ActiveRolePort activeRolePort;

	private JwtAuthenticationFilter filter;

	@BeforeEach
	void setUp() {
		SecurityContextHolder.clearContext();
		filter = new JwtAuthenticationFilter(jwtTokenDecoder, authUserAccountPort, activeRolePort);
	}

	@Test
	void staleSecurityVersionIsRejected() throws Exception {
		AuthenticatedUser tokenUser = new AuthenticatedUser(
				9L, "prof@example.com", "teacher", List.of("teacher"), false, 1);
		when(jwtTokenDecoder.decode("stale")).thenReturn(Optional.of(tokenUser));
		when(authUserAccountPort.findSessionState(9L)).thenReturn(Optional.of(new SessionState(true, 2)));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer stale");
		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void matchingSessionAuthenticates() throws Exception {
		AuthenticatedUser tokenUser = new AuthenticatedUser(
				9L, "prof@example.com", "teacher", List.of("teacher"), false, 2);
		when(jwtTokenDecoder.decode("ok")).thenReturn(Optional.of(tokenUser));
		when(authUserAccountPort.findSessionState(9L)).thenReturn(Optional.of(new SessionState(true, 2)));
		when(activeRolePort.findSpaceRoles(9L)).thenReturn(List.of("teacher"));
		when(activeRolePort.hasAdminRole(9L)).thenReturn(false);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer ok");
		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.isEqualTo(tokenUser);
	}

	@Test
	void revokedActiveRoleIsRejectedEvenWithMatchingVersion() throws Exception {
		AuthenticatedUser tokenUser = new AuthenticatedUser(
				9L, "prof@example.com", "teacher", List.of("teacher"), false, 0);
		when(jwtTokenDecoder.decode("role-gone")).thenReturn(Optional.of(tokenUser));
		when(authUserAccountPort.findSessionState(9L)).thenReturn(Optional.of(new SessionState(true, 0)));
		when(activeRolePort.findSpaceRoles(9L)).thenReturn(List.of("parent"));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer role-gone");
		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}
}
