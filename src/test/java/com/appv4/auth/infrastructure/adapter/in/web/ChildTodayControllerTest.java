package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.nullValue;

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

import com.appv4.auth.application.port.out.ChildDailyMissionPort;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredMission;
import com.appv4.auth.application.port.out.ChildDailyMissionPort.StoredTask;
import com.appv4.auth.application.port.out.ChildLoginPort;
import com.appv4.auth.application.port.out.ChildLoginPort.ChildLoginCandidate;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.service.ChildDailyMissionService;
import com.appv4.auth.application.service.ReadChildProgressService;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChildTodayControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ChildLoginPort childLoginPort;

	@MockBean
	private ChildSubjectPort childSubjectPort;

	@MockBean
	private ChildDailyMissionPort childDailyMissionPort;

	@Test
	void todayWithoutSubjectsIsEmpty() throws Exception {
		String token = loginAsLea();
		when(childLoginPort.findActiveById(7L))
				.thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "x")));
		when(childSubjectPort.linksOf(7L)).thenReturn(List.of());
		when(childDailyMissionPort.findByChildAndDate(eq(7L), any(LocalDate.class))).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/ext/today").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mission").value(nullValue()));
	}

	@Test
	void todayReturnsStoredMission() throws Exception {
		String token = loginAsLea();
		when(childLoginPort.findActiveById(7L))
				.thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "x")));
		LocalDate today = LocalDate.now(ReadChildProgressService.PROGRESS_ZONE);
		when(childDailyMissionPort.findByChildAndDate(eq(7L), eq(today))).thenReturn(Optional.of(new StoredMission(
				8L,
				7L,
				today,
				"Joue 1 question aujourd'hui",
				List.of(new StoredTask(3L, 1, "Maths", 1, 0, 10, 4L, 11L)))));

		mockMvc.perform(get("/api/ext/today").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mission.id").value(8))
				.andExpect(jsonPath("$.mission.objective").value("Joue 1 question aujourd'hui"))
				.andExpect(jsonPath("$.mission.progress").value(0))
				.andExpect(jsonPath("$.mission.target").value(1))
				.andExpect(jsonPath("$.mission.rewardXp").value(10))
				.andExpect(jsonPath("$.mission.status").value("en_cours"))
				.andExpect(jsonPath("$.mission.statusLabel").value("En cours"))
				.andExpect(jsonPath("$.mission.playable").value(true))
				.andExpect(jsonPath("$.mission.playSubjectId").value(4))
				.andExpect(jsonPath("$.mission.playQuestionId").value(11))
				.andExpect(jsonPath("$.mission.tasks[0].label").value("Maths"))
				.andExpect(jsonPath("$.mission.tasks[0].status").value("a_faire"))
				.andExpect(jsonPath("$.mission.tasks[0].subjectId").value(4))
				.andExpect(jsonPath("$.mission.tasks[0].questionId").value(11));
	}

	@Test
	void todayWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/ext/today"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(ChildDailyMissionService.INVALID_SESSION_MESSAGE));
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
