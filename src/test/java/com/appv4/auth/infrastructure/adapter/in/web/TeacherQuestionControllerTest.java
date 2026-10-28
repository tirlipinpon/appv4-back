package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoice;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPair;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.service.SpaceHello;
import com.appv4.auth.application.service.TeacherQuestionService;
import com.appv4.auth.application.service.TeacherSubjectService;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherQuestionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JjwtTokenAdapter tokens;

	@Autowired
	private TeacherSubjectPort subjects;

	@Autowired
	private TeacherQuestionPort questions;

	@Test
	void teacherSeesMultipleChoiceMemoryOrderAndTrueFalse() throws Exception {
		mockMvc.perform(get("/api/int/teacher/question-forms").header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.forms.length()").value(4))
				.andExpect(jsonPath("$.forms[0].code").value("qcm"))
				.andExpect(jsonPath("$.forms[0].label").value("Choix multiples"))
				.andExpect(jsonPath("$.forms[1].code").value("memory"))
				.andExpect(jsonPath("$.forms[1].label").value("Memory"))
				.andExpect(jsonPath("$.forms[2].code").value("order"))
				.andExpect(jsonPath("$.forms[2].label").value("Ordre"))
				.andExpect(jsonPath("$.forms[3].code").value("vrai_faux"))
				.andExpect(jsonPath("$.forms[3].label").value("Vrai ou faux"));
	}

	@Test
	void createReturnsTheSavedQuestion() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(questions.insert(eq(4L), eq("qcm"), eq("Combien font 2 + 2 ?"), any()))
				.thenReturn(new StoredQuestion(
						7L,
						4L,
						"qcm",
						"Combien font 2 + 2 ?",
						List.of(new StoredChoice(1L, "3", false), new StoredChoice(2L, "4", true)),
						List.of(),
						List.of()));

		mockMvc.perform(post("/api/int/teacher/subjects/4/questions")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"formCode":"qcm","prompt":"Combien font 2 + 2 ?","choices":[
								  {"label":"3","correct":false},
								  {"label":"4","correct":true},
								  {"label":"5","correct":false}
								]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.formCode").value("qcm"))
				.andExpect(jsonPath("$.formLabel").value("Choix multiples"))
				.andExpect(jsonPath("$.prompt").value("Combien font 2 + 2 ?"))
				.andExpect(jsonPath("$.choices[1].label").value("4"))
				.andExpect(jsonPath("$.choices[1].correct").value(true));
	}

	@Test
	void createMemoryReturnsThePairs() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(questions.insert(eq(4L), eq("memory"), eq(""), any()))
				.thenReturn(new StoredQuestion(
						8L,
						4L,
						"memory",
						"",
						List.of(),
						List.of(new StoredPair(1L, "2 + 2", "4"), new StoredPair(2L, "3 + 3", "6")),
						List.of()));

		mockMvc.perform(post("/api/int/teacher/subjects/4/questions")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"formCode":"memory","prompt":"","pairs":[
								  {"questionSide":"2 + 2","answerSide":"4"},
								  {"questionSide":"3 + 3","answerSide":"6"}
								]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(8))
				.andExpect(jsonPath("$.formCode").value("memory"))
				.andExpect(jsonPath("$.formLabel").value("Memory"))
				.andExpect(jsonPath("$.prompt").value(""))
				.andExpect(jsonPath("$.pairs[0].questionSide").value("2 + 2"))
				.andExpect(jsonPath("$.pairs[1].answerSide").value("6"));
	}

	@Test
	void createOrderReturnsTheItems() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(questions.insert(eq(4L), eq("order"), eq(""), any()))
				.thenReturn(new StoredQuestion(
						9L,
						4L,
						"order",
						"",
						List.of(),
						List.of(),
						List.of(
								new TeacherQuestionPort.StoredItem(1L, "Lundi"),
								new TeacherQuestionPort.StoredItem(2L, "Mardi"))));

		mockMvc.perform(post("/api/int/teacher/subjects/4/questions")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"formCode":"order","prompt":"","items":[
								  {"label":"Lundi"},
								  {"label":"Mardi"}
								]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(9))
				.andExpect(jsonPath("$.formCode").value("order"))
				.andExpect(jsonPath("$.formLabel").value("Ordre"))
				.andExpect(jsonPath("$.prompt").value(""))
				.andExpect(jsonPath("$.items[0].label").value("Lundi"))
				.andExpect(jsonPath("$.items[1].label").value("Mardi"));
	}

	@Test
	void createTrueFalseReturnsEachSide() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(questions.insert(eq(4L), eq("vrai_faux"), eq(""), any()))
				.thenReturn(new StoredQuestion(
						10L,
						4L,
						"vrai_faux",
						"",
						new TeacherQuestionPort.StoredStatements(List.of(
								new TeacherQuestionPort.StoredStatement(1L, "2 + 2 = 4", true),
								new TeacherQuestionPort.StoredStatement(2L, "3 + 3 = 5", false)))));

		mockMvc.perform(post("/api/int/teacher/subjects/4/questions")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"formCode":"vrai_faux","prompt":"","statements":[
								  {"label":"2 + 2 = 4","truthful":true},
								  {"label":"3 + 3 = 5","truthful":false}
								]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(10))
				.andExpect(jsonPath("$.formCode").value("vrai_faux"))
				.andExpect(jsonPath("$.formLabel").value("Vrai ou faux"))
				.andExpect(jsonPath("$.statements[0].label").value("2 + 2 = 4"))
				.andExpect(jsonPath("$.statements[0].truthful").value(true))
				.andExpect(jsonPath("$.statements[1].truthful").value(false));
	}

	@Test
	void updateReturnsTheCorrectedQuestion() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(questions.findInSubject(4L, 7L))
				.thenReturn(Optional.of(new StoredQuestion(
						7L,
						4L,
						"qcm",
						"2*2 ?",
						List.of(new StoredChoice(1L, "3", false), new StoredChoice(2L, "4", true)),
						List.of(),
						List.of())));
		when(questions.replace(eq(7L), eq("2*3 ?"), any()))
				.thenReturn(new StoredQuestion(
						7L,
						4L,
						"qcm",
						"2*3 ?",
						List.of(new StoredChoice(3L, "5", false), new StoredChoice(4L, "6", true)),
						List.of(),
						List.of()));

		mockMvc.perform(put("/api/int/teacher/subjects/4/questions/7")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"formCode":"qcm","prompt":"2*3 ?","choices":[
								  {"label":"5","correct":false},
								  {"label":"6","correct":true}
								]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.prompt").value("2*3 ?"))
				.andExpect(jsonPath("$.formCode").value("qcm"))
				.andExpect(jsonPath("$.choices[1].label").value("6"))
				.andExpect(jsonPath("$.choices[1].correct").value(true));
	}

	@Test
	void missingQuestionIsNotFound() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(questions.findInSubject(4L, 8L)).thenReturn(Optional.empty());

		mockMvc.perform(put("/api/int/teacher/subjects/4/questions/8")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"formCode":"qcm","prompt":"2*2 ?","choices":[
								  {"label":"3","correct":false},
								  {"label":"4","correct":true}
								]}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(TeacherQuestionService.QUESTION_MISSING));
	}

	@Test
	void blankPromptIsRejected() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));

		mockMvc.perform(post("/api/int/teacher/subjects/4/questions")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"formCode":"qcm","prompt":"  ","choices":[
								  {"label":"3","correct":false},
								  {"label":"4","correct":true}
								]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherQuestionService.PROMPT_REQUIRED));
	}

	@Test
	void unknownSubjectIsNotFound() throws Exception {
		when(subjects.findOwned(9L, 4L)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/int/teacher/subjects/4/questions")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value(TeacherSubjectService.SUBJECT_MISSING));
	}

	@Test
	void parentAndAnonymousCannotOpenQuestions() throws Exception {
		mockMvc.perform(get("/api/int/teacher/question-forms").header(HttpHeaders.AUTHORIZATION, bearer("parent")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(SpaceHello.DENIED));

		mockMvc.perform(get("/api/int/teacher/question-forms"))
				.andExpect(status().isUnauthorized());
	}

	private String bearer(String activeRole) {
		String raw = tokens.encode(9L, "prof@example.com", activeRole, List.of("teacher"), false, 0).token();
		return "Bearer " + raw;
	}
}
