package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.JwtTokenDecoder.AuthenticatedUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConnectedAccountControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AuthUserAccountPort authUserAccountPort;

	@Test
	void anonymousCallIsRejected() throws Exception {
		mockMvc.perform(get("/api/int/auth/me")).andExpect(status().isUnauthorized());
		verifyNoInteractions(authUserAccountPort);
	}

	@Test
	void returnsTheNameForAParentOrATeacher() throws Exception {
		when(authUserAccountPort.findById(4L))
				.thenReturn(Optional.of(new AuthUserAccount(4L, "anne@example.com", "Anne", true)));
		when(authUserAccountPort.findAvatarCode(4L)).thenReturn(Optional.of("owl"));

		mockMvc.perform(get("/api/int/auth/me").with(authentication(signedIn())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.displayName").value("Anne"))
				.andExpect(jsonPath("$.avatarCode").value("owl"));
	}

	private static UsernamePasswordAuthenticationToken signedIn() {
		return new UsernamePasswordAuthenticationToken(
				new AuthenticatedUser(4L, "anne@example.com", "teacher", List.of("parent", "teacher"), false, 0),
				null,
				List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}
}
