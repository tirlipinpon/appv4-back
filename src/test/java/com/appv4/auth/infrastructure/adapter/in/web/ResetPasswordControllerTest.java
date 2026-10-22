package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.port.in.ResetPasswordUseCase;
import com.appv4.auth.application.service.ResetPasswordService;

@SpringBootTest
@AutoConfigureMockMvc
class ResetPasswordControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ResetPasswordUseCase resetPasswordUseCase;

	@Test
	void resetOkReturnsSuccessMessage() throws Exception {
		mockMvc.perform(post("/api/int/auth/reset-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"token":"abc","newPassword":"Jardin1!"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value(ResetPasswordService.SUCCESS_MESSAGE));

		verify(resetPasswordUseCase).reset(eq("abc"), eq("Jardin1!"));
	}

	@Test
	void resetInvalidTokenReturns400() throws Exception {
		doThrow(new AuthDomainException(HttpStatus.BAD_REQUEST.value(), ResetPasswordService.INVALID_TOKEN_MESSAGE))
				.when(resetPasswordUseCase).reset(eq("bad"), eq("Jardin1!"));

		mockMvc.perform(post("/api/int/auth/reset-password")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"token":"bad","newPassword":"Jardin1!"}
								"""))
				.andExpect(status().isBadRequest());
	}
}
