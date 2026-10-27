package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.anyList;
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
	void teacherSeesOnlyMultipleChoice() throws Exception {
		mockMvc.perform(get("/api/int/teacher/question-forms").header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.forms.length()").value(1))
				.andExpect(jsonPath("$.forms[0].code").value("qcm"))
				.andExpect(jsonPath("$.forms[0].label").value("Choix multiples"));
	}

	@Test
	void createReturnsTheSavedQuestion() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(questions.insert(eq(4L), eq("qcm"), eq("Combien font 2 + 2 ?"), anyList()))
				.thenReturn(new StoredQuestion(
						7L,
						4L,
						"qcm",
						"Combien font 2 + 2 ?",
						List.of(new StoredChoice(1L, "3", false), new StoredChoice(2L, "4", true))));

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
	void updateReturnsTheCorrectedQuestion() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(questions.findInSubject(4L, 7L))
				.thenReturn(Optional.of(new StoredQuestion(
						7L,
						4L,
						"qcm",
						"2*2 ?",
						List.of(new StoredChoice(1L, "3", false), new StoredChoice(2L, "4", true)))));
		when(questions.replace(eq(7L), eq("2*3 ?"), anyList()))
				.thenReturn(new StoredQuestion(
						7L,
						4L,
						"qcm",
						"2*3 ?",
						List.of(new StoredChoice(3L, "5", false), new StoredChoice(4L, "6", true))));

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
