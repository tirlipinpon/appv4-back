package com.appv4.auth.infrastructure.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.service.SpaceHello;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;

@SpringBootTest
@AutoConfigureMockMvc
class SpaceHelloControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JjwtTokenAdapter tokens;

	@Test
	void parentTokenOpensParentHelloAndRefusesTeacher() throws Exception {
		String token = bearer("parent", List.of("parent"));

		mockMvc.perform(get("/api/int/parent/hello").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.text").value(SpaceHello.PARENT_TEXT));

		mockMvc.perform(get("/api/int/teacher/hello").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(SpaceHello.DENIED));
	}

	@Test
	void teacherTokenOpensTeacherHelloAndRefusesParent() throws Exception {
		String token = bearer("teacher", List.of("teacher"));

		mockMvc.perform(get("/api/int/teacher/hello").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.text").value(SpaceHello.TEACHER_TEXT));

		mockMvc.perform(get("/api/int/parent/hello").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(SpaceHello.DENIED));
	}

	@Test
	void missingActiveRoleRefusesBothDoors() throws Exception {
		String token = bearer(null, List.of("parent", "teacher"));

		mockMvc.perform(get("/api/int/parent/hello").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/int/teacher/hello").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isForbidden());
	}

	@Test
	void anonymousCallIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/int/parent/hello"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void generalAdminHelloStaysOpenForAnyToken() throws Exception {
		String token = bearer("teacher", List.of("teacher"));

		mockMvc.perform(get("/api/int/hello").header(HttpHeaders.AUTHORIZATION, token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.text").exists());
	}

	private String bearer(String activeRole, List<String> roles) {
		String raw = tokens.encode(9L, "space@example.com", activeRole, roles, false).token();
		return "Bearer " + raw;
	}
}
