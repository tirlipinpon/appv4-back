package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.port.out.ActiveRolePort;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;
import com.appv4.auth.application.port.out.SignupRolePort;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaceAccessControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JjwtTokenAdapter tokens;

	@MockBean
	private ActiveRolePort activeRolePort;

	@MockBean
	private SignupRolePort signupRolePort;

	@Test
	void anonymousCallsAreRejected() throws Exception {
		mockMvc.perform(post("/api/int/auth/spaces/parent")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/int/auth/spaces/teacher")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/int/auth/spaces")).andExpect(status().isUnauthorized());

		verifyNoInteractions(signupRolePort);
	}

	@Test
	void openParentReturnsATokenThatContainsParent() throws Exception {
		when(activeRolePort.loadAuthzSnapshot(4L)).thenReturn(
				new ActiveRolePort.AuthzSnapshot(List.of("parent"), false, null, false));

		String body = mockMvc.perform(post("/api/int/auth/spaces/parent").with(authentication(parentOnly())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andReturn()
				.getResponse()
				.getContentAsString();

		verify(signupRolePort).grantParent(4L);
		String token = body.replaceAll(".*\"token\"\\s*:\\s*\"([^\"]+)\".*", "$1");
		AuthenticatedUser user = tokens.decode(token).orElseThrow();
		org.junit.jupiter.api.Assertions.assertTrue(user.roles().contains("parent"));
		org.junit.jupiter.api.Assertions.assertFalse(user.roles().contains("teacher"));
	}

	@Test
	void requestTeacherDoesNotReturnATeacherRole() throws Exception {
		when(activeRolePort.findSpaceRoles(4L)).thenReturn(List.of("parent"));
		when(activeRolePort.teacherRequestStatus(4L)).thenReturn("pending");

		mockMvc.perform(post("/api/int/auth/spaces/teacher").with(authentication(parentOnly())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").doesNotExist())
				.andExpect(jsonPath("$.roles[0]").value("parent"))
				.andExpect(jsonPath("$.teacherRequest").value("pending"));

		verify(signupRolePort).requestTeacher(4L);
	}

	@Test
	void secondTeacherRequestStaysASinglePendingRead() throws Exception {
		when(activeRolePort.findSpaceRoles(4L)).thenReturn(List.of("parent"));
		when(activeRolePort.teacherRequestStatus(4L)).thenReturn("pending");

		mockMvc.perform(post("/api/int/auth/spaces/teacher").with(authentication(parentOnly())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.teacherRequest").value("pending"));
	}

	@Test
	void viewTellsTheHeaderThereIsNoRequestYet() throws Exception {
		when(activeRolePort.findSpaceRoles(4L)).thenReturn(List.of("parent"));
		when(activeRolePort.teacherRequestStatus(4L)).thenReturn("none");

		mockMvc.perform(get("/api/int/auth/spaces").with(authentication(parentOnly())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roles[0]").value("parent"))
				.andExpect(jsonPath("$.teacherRequest").value("none"));
	}

	private static UsernamePasswordAuthenticationToken parentOnly() {
		return new UsernamePasswordAuthenticationToken(
				new AuthenticatedUser(4L, "parent@example.com", "parent", List.of("parent"), false, 0),
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}
}
