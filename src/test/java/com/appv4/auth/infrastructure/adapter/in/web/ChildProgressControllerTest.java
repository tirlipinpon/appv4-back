package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.appv4.auth.application.port.out.ChildProgressPort;
import com.appv4.auth.application.port.out.ChildProgressPort.StoredProgress;
import com.appv4.auth.application.service.ReadChildProgressService;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChildProgressControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ChildLoginPort childLoginPort;

	@MockBean
	private ChildProgressPort childProgressPort;

	@Test
	void progressReturnsZerosWhenNoRow() throws Exception {
		String token = loginAsLea();
		when(childLoginPort.findActiveById(7L))
				.thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "x")));
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/ext/progress").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.xpTotal").value(0))
				.andExpect(jsonPath("$.currentStreak").value(0))
				.andExpect(jsonPath("$.leagueLabel").value("Bronze"))
				.andExpect(jsonPath("$.weekDays.length()").value(7))
				.andExpect(jsonPath("$.weekDays[0]").value(false));
	}

	@Test
	void progressReturnsStoredValues() throws Exception {
		String token = loginAsLea();
		when(childLoginPort.findActiveById(7L))
				.thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "x")));
		LocalDate today = LocalDate.now(ReadChildProgressService.PROGRESS_ZONE);
		LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
		char[] bits = "0000000".toCharArray();
		bits[today.getDayOfWeek().getValue() - 1] = '1';
		when(childProgressPort.findByChildId(7L)).thenReturn(Optional.of(
				new StoredProgress(7L, 160, 3, today, monday, new String(bits))));

		mockMvc.perform(get("/api/ext/progress").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.xpTotal").value(160))
				.andExpect(jsonPath("$.currentStreak").value(3))
				.andExpect(jsonPath("$.leagueLabel").value("Or"));
	}

	@Test
	void progressWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/ext/progress"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(ReadChildProgressService.INVALID_SESSION_MESSAGE));
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
