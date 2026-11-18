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
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.NewAttempt;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredAttempt;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoice;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoices;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredItem;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredItems;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPair;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPairs;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredStatement;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredStatements;
import com.appv4.auth.application.service.ChildQuestionPlayService;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChildQuestionPlayControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ChildLoginPort childLoginPort;

	@MockBean
	private ChildSubjectPort childSubjectPort;

	@MockBean
	private TeacherQuestionPort teacherQuestionPort;

	@MockBean
	private ChildQuestionAttemptPort childQuestionAttemptPort;

	@Test
	void getTrueFalseHidesTruthful() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 11L)).thenReturn(Optional.of(new StoredQuestion(
				11L,
				4L,
				"vrai_faux",
				"Coche le vrai",
				new StoredStatements(List.of(
						new StoredStatement(1L, "2+2=4", true),
						new StoredStatement(2L, "2+2=5", false))))));

		mockMvc.perform(get("/api/ext/subjects/4/questions/11").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.formCode").value("vrai_faux"))
				.andExpect(jsonPath("$.statements[0].statementId").value(1))
				.andExpect(jsonPath("$.statements[0].label").value("2+2=4"))
				.andExpect(jsonPath("$.statements[0].truthful").doesNotExist())
				.andExpect(jsonPath("$.statements[1].statementId").value(2));
	}

	@Test
	void submitTrueFalseSuccessStoresAttempt() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 11L)).thenReturn(Optional.of(new StoredQuestion(
				11L,
				4L,
				"vrai_faux",
				"",
				new StoredStatements(List.of(
						new StoredStatement(1L, "2+2=4", true),
						new StoredStatement(2L, "2+2=5", false))))));
		when(childQuestionAttemptPort.insert(any(NewAttempt.class)))
				.thenReturn(new StoredAttempt(42L, 7L, 4L, 11L, "vrai_faux", 100, true));

		mockMvc.perform(post("/api/ext/subjects/4/questions/11/attempts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"statements":[
								{"statementId":1,"value":true},
								{"statementId":2,"value":false}
								]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.attemptId").value(42))
				.andExpect(jsonPath("$.scorePercent").value(100))
				.andExpect(jsonPath("$.passed").value(true));
	}

	@Test
	void getMemoryReturnsPairs() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 13L)).thenReturn(Optional.of(new StoredQuestion(
				13L,
				4L,
				"memory",
				"",
				new StoredPairs(List.of(
						new StoredPair(1L, "Chat", "Miaou"),
						new StoredPair(2L, "Pomme", "Fruit"))))));

		mockMvc.perform(get("/api/ext/subjects/4/questions/13").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.formCode").value("memory"))
				.andExpect(jsonPath("$.pairs[0].pairId").value(1))
				.andExpect(jsonPath("$.pairs[0].questionSide").value("Chat"))
				.andExpect(jsonPath("$.pairs[0].answerSide").value("Miaou"))
				.andExpect(jsonPath("$.pairs[1].pairId").value(2));
	}

	@Test
	void submitMemoryAllPairsPasses() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 13L)).thenReturn(Optional.of(new StoredQuestion(
				13L,
				4L,
				"memory",
				"",
				new StoredPairs(List.of(
						new StoredPair(1L, "Chat", "Miaou"),
						new StoredPair(2L, "Pomme", "Fruit"))))));
		when(childQuestionAttemptPort.insert(any(NewAttempt.class)))
				.thenReturn(new StoredAttempt(44L, 7L, 4L, 13L, "memory", 100, true));

		mockMvc.perform(post("/api/ext/subjects/4/questions/13/attempts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"matchedPairIds":[1,2]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.attemptId").value(44))
				.andExpect(jsonPath("$.scorePercent").value(100))
				.andExpect(jsonPath("$.passed").value(true));
	}

	@Test
	void getOrderReturnsItems() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 14L)).thenReturn(Optional.of(new StoredQuestion(
				14L,
				4L,
				"order",
				"",
				new StoredItems(List.of(
						new StoredItem(1L, "Lundi"),
						new StoredItem(2L, "Mardi"),
						new StoredItem(3L, "Mercredi"))))));

		mockMvc.perform(get("/api/ext/subjects/4/questions/14").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.formCode").value("order"))
				.andExpect(jsonPath("$.items.length()").value(3))
				.andExpect(jsonPath("$.items[?(@.itemId == 1)].label").value("Lundi"));
	}

	@Test
	void submitOrderExactPasses() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 14L)).thenReturn(Optional.of(new StoredQuestion(
				14L,
				4L,
				"order",
				"",
				new StoredItems(List.of(
						new StoredItem(1L, "Lundi"),
						new StoredItem(2L, "Mardi"),
						new StoredItem(3L, "Mercredi"))))));
		when(childQuestionAttemptPort.insert(any(NewAttempt.class)))
				.thenReturn(new StoredAttempt(45L, 7L, 4L, 14L, "order", 100, true));

		mockMvc.perform(post("/api/ext/subjects/4/questions/14/attempts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"orderedItemIds":[1,2,3]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.attemptId").value(45))
				.andExpect(jsonPath("$.scorePercent").value(100))
				.andExpect(jsonPath("$.passed").value(true));
	}

	@Test
	void submitQcmWrongIsZero() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 12L)).thenReturn(Optional.of(new StoredQuestion(
				12L,
				4L,
				"qcm",
				"Combien ?",
				new StoredChoices(List.of(
						new StoredChoice(10L, "3", false),
						new StoredChoice(11L, "4", true))))));
		when(childQuestionAttemptPort.insert(any(NewAttempt.class)))
				.thenReturn(new StoredAttempt(43L, 7L, 4L, 12L, "qcm", 0, false));

		mockMvc.perform(post("/api/ext/subjects/4/questions/12/attempts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"selectedChoiceIds":[10]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.scorePercent").value(0))
				.andExpect(jsonPath("$.passed").value(false));
	}

	@Test
	void unlinkedSubjectIsNotFound() throws Exception {
		String token = loginAsLea();
		when(childLoginPort.findActiveById(7L))
				.thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "hash")));
		when(childSubjectPort.linksOf(7L)).thenReturn(List.of());

		mockMvc.perform(get("/api/ext/subjects/4/questions/11").header("Authorization", "Bearer " + token))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(ChildQuestionPlayService.SUBJECT_MISSING));
	}

	private void stubLinkedChild() {
		when(childLoginPort.findActiveById(7L))
				.thenReturn(Optional.of(new ChildLoginCandidate(7L, "Léa", "hash")));
		when(childSubjectPort.linksOf(7L)).thenReturn(List.of(new StoredLink(4L, false)));
		when(childSubjectPort.visibleByIds(eq(List.of(4L))))
				.thenReturn(List.of(new CatalogSubject(4L, "Mathématiques", "Mme X", 1L, "CE1", "math")));
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
