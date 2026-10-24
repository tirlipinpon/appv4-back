package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.port.out.AuthUserAccountPort;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"appv4.auth.rate-limit.enabled=true",
		"appv4.auth.rate-limit.max-requests=2",
		"appv4.auth.rate-limit.window-seconds=60"
})
class AuthRateLimitFilterTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private AuthUserAccountPort authUserAccountPort;

	@Test
	void thirdLoginFromSameClientReturns429() throws Exception {
		when(authUserAccountPort.findLoginCandidateByEmail(anyString())).thenReturn(Optional.empty());
		String body = "{\"email\":\"a@b.com\",\"password\":\"Secret1!\"}";

		mockMvc.perform(post("/api/int/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/int/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/int/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isTooManyRequests());
	}
}
