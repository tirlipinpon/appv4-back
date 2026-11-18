package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import com.appv4.auth.application.port.out.ChildSubjectBoardPort;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort.BoardMeta;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort.LessonQuestion;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort.StoredLesson;
import com.appv4.auth.application.port.out.ChildSubjectBoardPort.StoredTopic;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.service.ReadChildSubjectBoardService;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChildSubjectBoardControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ChildLoginPort childLoginPort;

	@MockBean
	private ChildSubjectPort childSubjectPort;

	@MockBean
	private ChildSubjectBoardPort childSubjectBoardPort;

	@Test
	void boardReturnsPayload() throws Exception {
		String token = loginAsLea();
		when(childLoginPort.findActiveById(7L))
				.thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "x")));
		when(childSubjectPort.linksOf(7L)).thenReturn(List.of(new StoredLink(5L, false)));
		when(childSubjectPort.visibleByIds(eq(List.of(5L))))
				.thenReturn(List.of(new CatalogSubject(5L, "Maths", "Mme X", 1L, "CE1", "math")));
		when(childSubjectBoardPort.findMeta(5L)).thenReturn(Optional.of(new BoardMeta(
				"Desc", "Tag", "WORLD", "5–9", "Facile")));
		when(childSubjectBoardPort.topicsOf(5L)).thenReturn(List.of(new StoredTopic(1L, 5L, "Nombres", "math", 0)));
		when(childSubjectBoardPort.lessonsOfTopics(any())).thenReturn(List.of(
				new StoredLesson(9L, 1L, "Leçon 1", 0, 5, 50)));
		when(childSubjectBoardPort.questionsOfLessons(any())).thenReturn(List.of(new LessonQuestion(9L, 42L, 0)));
		when(childSubjectBoardPort.passedQuestionIds(7L, 5L)).thenReturn(Set.of());
		when(childSubjectBoardPort.xpEarned(7L, 5L)).thenReturn(0);

		mockMvc.perform(get("/api/ext/subjects/5").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subjectId").value(5))
				.andExpect(jsonPath("$.name").value("Maths"))
				.andExpect(jsonPath("$.worldLabel").value("WORLD"))
				.andExpect(jsonPath("$.lessonsTotal").value(1))
				.andExpect(jsonPath("$.continueLesson.title").value("Leçon 1"))
				.andExpect(jsonPath("$.continueLesson.playQuestionId").value(42))
				.andExpect(jsonPath("$.topics[0].name").value("Nombres"));
	}

	@Test
	void unlinkedSubjectIsNotFound() throws Exception {
		String token = loginAsLea();
		when(childLoginPort.findActiveById(7L))
				.thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "x")));
		when(childSubjectPort.linksOf(7L)).thenReturn(List.of());

		mockMvc.perform(get("/api/ext/subjects/5").header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(ReadChildSubjectBoardService.SUBJECT_MISSING));
	}

	@Test
	void boardWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/ext/subjects/5"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(ReadChildSubjectBoardService.INVALID_SESSION_MESSAGE));
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
