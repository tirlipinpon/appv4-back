package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.JwtTokenEncoder;
import com.appv4.auth.application.service.LoginChildService;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChildLoginControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenEncoder jwtTokenEncoder;

	@MockBean
	private ChildLoginPort childLoginPort;

	@Test
	void loginReturnsChildTokenAndSessionReadsTheFirstName() throws Exception {
		String hash = new BCryptPasswordEncoder().encode("1234");
		when(childLoginPort.findActiveByFirstName(eq("léa")))
				.thenReturn(List.of(new ChildLoginCandidate(7L, "Léa", hash)));
		when(childLoginPort.findActiveById(7L))
				.thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", hash)));

		MvcResult login = mockMvc.perform(post("/api/ext/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"firstName":"Léa","pin":"1234"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.firstName").value("Léa"))
				.andExpect(jsonPath("$.expiresAtEpochSeconds").isNumber())
				.andReturn();
		String token = JsonPath.read(login.getResponse().getContentAsString(), "$.token");

		mockMvc.perform(get("/api/ext/auth/session").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.childId").value(7))
				.andExpect(jsonPath("$.firstName").value("Léa"));
	}

	@Test
	void unknownChildAndWrongPinShareTheSameMessage() throws Exception {
		when(childLoginPort.findActiveByFirstName(eq("ghost"))).thenReturn(List.of());
		String hash = new BCryptPasswordEncoder().encode("1234");
		when(childLoginPort.findActiveByFirstName(eq("léa")))
				.thenReturn(List.of(new ChildLoginCandidate(7L, "Léa", hash)));

		mockMvc.perform(post("/api/ext/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"firstName":"Ghost","pin":"1234"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(LoginChildService.INVALID_CREDENTIALS_MESSAGE));

		mockMvc.perform(post("/api/ext/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"firstName":"Léa","pin":"0000"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(LoginChildService.INVALID_CREDENTIALS_MESSAGE));
	}

	@Test
	void adultTokenDoesNotOpenAChildSession() throws Exception {
		String adult = jwtTokenEncoder.encode(3L, "parent@example.com", "parent", List.of("parent"), false, 0)
				.token();

		mockMvc.perform(get("/api/ext/auth/session").header("Authorization", "Bearer " + adult))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(LoginChildService.INVALID_SESSION_MESSAGE));
	}

	@Test
	void childTokenDoesNotOpenTheAdmin() throws Exception {
		String hash = new BCryptPasswordEncoder().encode("1234");
		when(childLoginPort.findActiveByFirstName(eq("léa")))
				.thenReturn(List.of(new ChildLoginCandidate(7L, "Léa", hash)));

		MvcResult login = mockMvc.perform(post("/api/ext/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"firstName":"Léa","pin":"1234"}
								"""))
				.andExpect(status().isOk())
				.andReturn();
		String token = JsonPath.read(login.getResponse().getContentAsString(), "$.token");

		mockMvc.perform(get("/api/int/parent/children").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sessionWithoutTokenIsRejected() throws Exception {
		mockMvc.perform(get("/api/ext/auth/session"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(LoginChildService.INVALID_SESSION_MESSAGE));
	}
}
