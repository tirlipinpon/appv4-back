package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.appv4.auth.application.port.out.ChildLoginPort.ChildPhotoBytes;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildProfile;
import com.appv4.auth.application.service.ReadChildProfileService;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChildProfileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ChildLoginPort childLoginPort;

	@Test
	void meReturnsProfileForChildToken() throws Exception {
		String token = loginAsLea();
		when(childLoginPort.findActiveProfileById(7L))
				.thenReturn(Optional.of(new ChildProfile(7L, "Léa", "fox", false, 8)));

		mockMvc.perform(get("/api/ext/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.childId").value(7))
				.andExpect(jsonPath("$.firstName").value("Léa"))
				.andExpect(jsonPath("$.avatarCode").value("fox"))
				.andExpect(jsonPath("$.hasPhoto").value(false));
	}

	@Test
	void photoReturnsBinaryWhenPresent() throws Exception {
		String token = loginAsLea();
		when(childLoginPort.findActiveProfileById(7L))
				.thenReturn(Optional.of(new ChildProfile(7L, "Léa", null, true, 8)));
		when(childLoginPort.findActivePhotoById(7L))
				.thenReturn(Optional.of(new ChildPhotoBytes(new byte[] {9, 8}, "image/png")));

		mockMvc.perform(get("/api/ext/me/photo").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "image/png"))
				.andExpect(content().bytes(new byte[] {9, 8}));
	}

	@Test
	void meWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/ext/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(ReadChildProfileService.INVALID_SESSION_MESSAGE));
	}

	@Test
	void photoMissingIsNotFound() throws Exception {
		String token = loginAsLea();
		when(childLoginPort.findActiveProfileById(7L))
				.thenReturn(Optional.of(new ChildProfile(7L, "Léa", "fox", false, 8)));
		when(childLoginPort.findActivePhotoById(7L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/ext/me/photo").header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(ReadChildProfileService.PHOTO_MISSING_MESSAGE));
	}

	private String loginAsLea() throws Exception {
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
		return JsonPath.read(login.getResponse().getContentAsString(), "$.token");
	}
}
