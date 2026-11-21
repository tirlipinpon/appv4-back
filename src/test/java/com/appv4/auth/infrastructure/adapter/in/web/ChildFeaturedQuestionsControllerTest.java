package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
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
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.JwtTokenEncoder;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.QuestionSummary;
import com.appv4.auth.application.service.ReadChildLearningSubjectsService;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChildFeaturedQuestionsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtTokenEncoder jwtTokenEncoder;

	@MockBean
	private ChildLoginPort childLoginPort;

	@MockBean
	private ChildSubjectPort childSubjectPort;

	@MockBean
	private TeacherQuestionPort teacherQuestionPort;

	@MockBean
	private ChildQuestionAttemptPort childQuestionAttemptPort;

	@Test
	void featuredWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/ext/featured"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(ReadChildLearningSubjectsService.INVALID_SESSION_MESSAGE));
	}

	@Test
	void adultTokenIsUnauthorizedLikeTheSubjectList() throws Exception {
		String adult = jwtTokenEncoder
				.encode(3L, "parent@example.com", "parent", List.of("parent"), false, 0)
				.token();

		mockMvc.perform(get("/api/ext/featured").header("Authorization", "Bearer " + adult))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(ReadChildLearningSubjectsService.INVALID_SESSION_MESSAGE));
	}

	@Test
	void featuredReturnsOnlySubjectQuestionAndPrompt() throws Exception {
		String token = loginAsLea();
		when(childLoginPort.findActiveById(7L))
				.thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "hash")));
		when(childSubjectPort.linksOf(7L)).thenReturn(List.of(new StoredLink(4L, false)));
		when(childSubjectPort.visibleByIds(List.of(4L)))
				.thenReturn(List.of(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math")));
		when(teacherQuestionPort.findSummariesBySubject(4L, 0L, 100)).thenReturn(List.of(
				new QuestionSummary(11L, "qcm", "Combien font 2 + 2 ?")));
		when(childQuestionAttemptPort.marksFor(eq(7L), eq(List.of(11L)))).thenReturn(Map.of());

		mockMvc.perform(get("/api/ext/featured").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].subjectId").value(4))
				.andExpect(jsonPath("$[0].questionId").value(11))
				.andExpect(jsonPath("$[0].prompt").value("Combien font 2 + 2 ?"))
				.andExpect(jsonPath("$[0].mark").doesNotExist())
				.andExpect(jsonPath("$[0].formCode").doesNotExist())
				.andExpect(jsonPath("$[0].formLabel").doesNotExist())
				.andExpect(jsonPath("$[0].yellowCount").doesNotExist());
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
