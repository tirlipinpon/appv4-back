package com.appv4.auth.infrastructure.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.port.in.IssueJwtUseCase;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntHelloSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private IssueJwtUseCase issueJwtUseCase;

	@Test
	void intHelloWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/int/hello"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void intHelloWithValidBearerReturns200() throws Exception {
		String token = issueJwtUseCase.issue(1L, "admin@example.com").token();

		mockMvc.perform(get("/api/int/hello")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.text").exists());
	}

	@Test
	void extHelloRemainsPublic() throws Exception {
		mockMvc.perform(get("/api/ext/hello"))
				.andExpect(status().isOk());
	}
}
