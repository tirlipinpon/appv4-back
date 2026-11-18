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
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoices;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.service.ReadChildLearningSubjectsService;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChildLearningSubjectsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ChildLoginPort childLoginPort;

	@MockBean
	private ChildSubjectPort childSubjectPort;

	@MockBean
	private TeacherQuestionPort teacherQuestionPort;

	@Test
	void listReturnsLinkedSubjectsWithIcon() throws Exception {
		String token = loginAsLea();
		when(childLoginPort.findActiveById(7L))
				.thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "hash")));
		when(childSubjectPort.linksOf(7L)).thenReturn(List.of(new StoredLink(4L, false)));
		when(childSubjectPort.visibleByIds(List.of(4L)))
				.thenReturn(List.of(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math")));
		when(teacherQuestionPort.countBySubjectIds(List.of(4L))).thenReturn(Map.of(4L, 3L));

		mockMvc.perform(get("/api/ext/subjects").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subjects[0].subjectId").value(4))
				.andExpect(jsonPath("$.subjects[0].name").value("Mathématiques"))
				.andExpect(jsonPath("$.subjects[0].grade").value("CE1"))
				.andExpect(jsonPath("$.subjects[0].questionCount").value(3))
				.andExpect(jsonPath("$.subjects[0].iconCode").value("math"));
	}

	@Test
	void questionsRequireLinkedSubject() throws Exception {
		String token = loginAsLea();
		when(childLoginPort.findActiveById(7L))
				.thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "hash")));
		when(childSubjectPort.linksOf(7L)).thenReturn(List.of());

		mockMvc.perform(get("/api/ext/subjects/4/questions").header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(ReadChildLearningSubjectsService.SUBJECT_MISSING));
	}

	@Test
	void questionsReturnPrompts() throws Exception {
		String token = loginAsLea();
		when(childLoginPort.findActiveById(7L))
				.thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "hash")));
		when(childSubjectPort.linksOf(7L)).thenReturn(List.of(new StoredLink(4L, false)));
		when(childSubjectPort.visibleByIds(List.of(4L)))
				.thenReturn(List.of(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math")));
		when(teacherQuestionPort.findBySubject(4L, 0L, 200)).thenReturn(List.of(
				new StoredQuestion(11L, 4L, "qcm", "Combien font 2+2 ?", new StoredChoices(List.of()))));

		mockMvc.perform(get("/api/ext/subjects/4/questions").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subjectId").value(4))
				.andExpect(jsonPath("$.subjectName").value("Mathématiques"))
				.andExpect(jsonPath("$.questions[0].questionId").value(11))
				.andExpect(jsonPath("$.questions[0].formCode").value("qcm"))
				.andExpect(jsonPath("$.questions[0].prompt").value("Combien font 2+2 ?"));
	}

	@Test
	void listWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/ext/subjects"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(ReadChildLearningSubjectsService.INVALID_SESSION_MESSAGE));
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
