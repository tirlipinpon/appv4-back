package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.TestPassword;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserLoginCandidate;
import com.appv4.auth.application.port.out.AuthUserAccountPort.SessionState;
import com.appv4.auth.application.service.LoginWithEmailService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AuthUserAccountPort authUserAccountPort;

	@BeforeEach
	void openSession() {
		when(authUserAccountPort.findSessionState(anyLong()))
				.thenReturn(Optional.of(new SessionState(true, 0)));
	}

	@Test
	void loginReturnsOkAndJwt() throws Exception {
		String hash = new BCryptPasswordEncoder().encode(TestPassword.VALID);
		when(authUserAccountPort.findLoginCandidateByEmail(eq("marie@example.com")))
				.thenReturn(Optional.of(new AuthUserLoginCandidate(
						42L, "marie@example.com", hash, true)));

		mockMvc.perform(post("/api/int/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"marie@example.com","password":"%s"}
								""".formatted(TestPassword.VALID)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.expiresAtEpochSeconds").isNumber());
	}

	@Test
	void loginUnknownEmailReturnsUnauthorizedWithSameMessage() throws Exception {
		when(authUserAccountPort.findLoginCandidateByEmail(eq("ghost@example.com")))
				.thenReturn(Optional.empty());

		mockMvc.perform(post("/api/int/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"ghost@example.com","password":"%s"}
								""".formatted(TestPassword.VALID)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(LoginWithEmailService.INVALID_CREDENTIALS_MESSAGE));
	}

	@Test
	void loginWrongPasswordReturnsUnauthorizedWithSameMessage() throws Exception {
		String hash = new BCryptPasswordEncoder().encode(TestPassword.VALID);
		when(authUserAccountPort.findLoginCandidateByEmail(eq("marie@example.com")))
				.thenReturn(Optional.of(new AuthUserLoginCandidate(
						42L, "marie@example.com", hash, true)));

		mockMvc.perform(post("/api/int/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"marie@example.com","password":"Wrong1!"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(LoginWithEmailService.INVALID_CREDENTIALS_MESSAGE));
	}

	@Test
	void loginRejectsInvalidBody() throws Exception {
		mockMvc.perform(post("/api/int/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"","password":"%s"}
								""".formatted(TestPassword.VALID)))
				.andExpect(status().isBadRequest());
	}
}
