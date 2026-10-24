package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.service.SwitchActiveRoleService;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwitchRoleControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JjwtTokenAdapter tokens;

	@MockBean
	private ActiveRolePort activeRolePort;

	private final AtomicReference<String> remembered = new AtomicReference<>("teacher");

	@BeforeEach
	void rememberRoles() {
		when(activeRolePort.findSpaceRoles(7L)).thenReturn(List.of("parent", "teacher"));
		when(activeRolePort.findLastActiveRole(7L)).thenAnswer(invocation -> Optional.ofNullable(remembered.get()));
		when(activeRolePort.loadAuthzSnapshot(7L)).thenAnswer(invocation ->
				new com.appv4.auth.application.port.out.ActiveRolePort.AuthzSnapshot(
						List.of("parent", "teacher"), false, remembered.get(), false));
		Mockito.doAnswer(invocation -> {
			remembered.set(invocation.getArgument(1));
			return null;
		}).when(activeRolePort).saveLastActiveRole(eq(7L), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void switchRoleReturnsATokenForTheChosenSpace() throws Exception {
		String body = mockMvc.perform(post("/api/int/auth/switch-role")
						.with(authentication(parentAndTeacher()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"role\":\"parent\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andReturn()
				.getResponse()
				.getContentAsString();

		String token = body.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
		AuthenticatedUser user = tokens.decode(token).orElseThrow();
		org.junit.jupiter.api.Assertions.assertEquals("parent", user.activeRole());
	}

	@Test
	void switchToMissingRoleIsForbidden() throws Exception {
		when(activeRolePort.findSpaceRoles(7L)).thenReturn(List.of("parent"));

		mockMvc.perform(post("/api/int/auth/switch-role")
						.with(authentication(parentAndTeacher()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"role\":\"teacher\"}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(SwitchActiveRoleService.ROLE_NOT_GRANTED));

		verify(activeRolePort, never()).saveLastActiveRole(anyLong(), isNull());
		verify(activeRolePort, never()).saveLastActiveRole(eq(7L), eq("teacher"));
	}

	@Test
	void anonymousCallIsRejected() throws Exception {
		mockMvc.perform(post("/api/int/auth/switch-role")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"role\":\"parent\"}"))
				.andExpect(status().isUnauthorized());
	}

	private static UsernamePasswordAuthenticationToken parentAndTeacher() {
		return new UsernamePasswordAuthenticationToken(
				new AuthenticatedUser(
						7L, "both@example.com", "teacher", List.of("parent", "teacher"), false, 0),
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}
}
