package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserSignupState;

@SpringBootTest
@AutoConfigureMockMvc
class SignupControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AuthUserAccountPort authUserAccountPort;

	@Test
	void signupReturnsCreatedAndJwt() throws Exception {
		when(authUserAccountPort.findSignupStateByEmail("new@example.com")).thenReturn(Optional.empty());
		when(authUserAccountPort.createWithPassword(
				eq("new@example.com"), eq("New"), org.mockito.ArgumentMatchers.anyString()))
				.thenReturn(new AuthUserAccount(42L, "new@example.com", "New", true));

		mockMvc.perform(post("/api/int/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"new@example.com\",\"password\":\"Secret1!\","
								+ "\"displayName\":\"New\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.expiresAtEpochSeconds").isNumber());
	}

	@Test
	void signupConflictWhenSocialExists() throws Exception {
		when(authUserAccountPort.findSignupStateByEmail("social@example.com"))
				.thenReturn(Optional.of(new AuthUserSignupState(
						9L, "social@example.com", false, List.of("google"))));

		mockMvc.perform(post("/api/int/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"social@example.com\",\"password\":\"Secret1!\","
								+ "\"displayName\":\"S\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Google")));
	}

	@Test
	void signupRejectsInvalidBody() throws Exception {
		mockMvc.perform(post("/api/int/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"","password":"Secret1!"}
								"""))
				.andExpect(status().isBadRequest());
	}
}
