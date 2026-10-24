package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.PasswordResetMailPort;
import com.appv4.auth.application.port.out.PasswordResetTokenPort;
import com.appv4.auth.application.service.ForgotPasswordService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ForgotPasswordControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AuthUserAccountPort authUserAccountPort;

	@MockBean
	private PasswordResetTokenPort passwordResetTokenPort;

	@MockBean
	private PasswordResetMailPort passwordResetMailPort;

	@Test
	void forgotReturnsGenericOkEvenWhenUnknown() throws Exception {
		when(authUserAccountPort.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

		mockMvc.perform(post("/api/int/auth/forgot-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"ghost@example.com"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value(ForgotPasswordService.PUBLIC_MESSAGE));
	}

	@Test
	void forgotKnownEmailTriggersTokenCreation() throws Exception {
		when(authUserAccountPort.findByEmail("marie@example.com"))
				.thenReturn(Optional.of(new AuthUserAccount(3L, "marie@example.com", "Marie", true)));

		mockMvc.perform(post("/api/int/auth/forgot-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"marie@example.com"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value(ForgotPasswordService.PUBLIC_MESSAGE));

		verify(passwordResetTokenPort).invalidateUnusedForUser(eq(3L));
		verify(passwordResetTokenPort).create(eq(3L), org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.any());
		verify(passwordResetMailPort).sendResetLink(eq("marie@example.com"),
				org.mockito.ArgumentMatchers.anyString());
	}
}
