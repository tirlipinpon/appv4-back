package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
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

import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.FollowingChild;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickPoint;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoice;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickImage;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickZone;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPair;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPuzzle;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPuzzlePiece;
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

	@Autowired
	private ChildSubjectPort children;

	@Test
	void teacherSeesMultipleChoiceMemoryOrderAndTrueFalse() throws Exception {
		mockMvc.perform(get("/api/int/teacher/question-forms").header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.forms.length()").value(9))
				.andExpect(jsonPath("$.forms[0].code").value("qcm"))
				.andExpect(jsonPath("$.forms[0].label").value("Choix multiples"))
				.andExpect(jsonPath("$.forms[1].code").value("memory"))
				.andExpect(jsonPath("$.forms[1].label").value("Memory"))
				.andExpect(jsonPath("$.forms[2].code").value("order"))
				.andExpect(jsonPath("$.forms[2].label").value("Ordre"))
				.andExpect(jsonPath("$.forms[3].code").value("vrai_faux"))
				.andExpect(jsonPath("$.forms[3].label").value("Vrai ou faux"))
				.andExpect(jsonPath("$.forms[4].code").value("liens"))
				.andExpect(jsonPath("$.forms[4].label").value("Liens"))
				.andExpect(jsonPath("$.forms[5].code").value("case_vide"))
				.andExpect(jsonPath("$.forms[5].label").value("Case vide"))
				.andExpect(jsonPath("$.forms[6].code").value("image_interactive"))
				.andExpect(jsonPath("$.forms[6].label").value("Image à cliquer"))
				.andExpect(jsonPath("$.forms[6].sortOrder").value(8))
				.andExpect(jsonPath("$.forms[7].code").value("puzzle"))
				.andExpect(jsonPath("$.forms[7].label").value("Puzzle"))
				.andExpect(jsonPath("$.forms[7].sortOrder").value(10))
				.andExpect(jsonPath("$.forms[8].code").value("texte"))
				.andExpect(jsonPath("$.forms[8].label").value("Réponse libre"))
				.andExpect(jsonPath("$.forms[8].sortOrder").value(11));
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
	void createLinksReturnsEachCouple() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(questions.insert(eq(4L), eq("liens"), eq("Relie."), any()))
				.thenReturn(new StoredQuestion(
						11L,
						4L,
						"liens",
						"Relie.",
						new TeacherQuestionPort.StoredLinks(List.of(
								new TeacherQuestionPort.StoredLink(1L, "Pomme", "Fruit"),
								new TeacherQuestionPort.StoredLink(2L, "Chien", "Animal")))));

		mockMvc.perform(post("/api/int/teacher/subjects/4/questions")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"formCode":"liens","prompt":"Relie.","links":[
								  {"leftLabel":"Pomme","rightLabel":"Fruit"},
								  {"leftLabel":"Chien","rightLabel":"Animal"}
								]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(11))
				.andExpect(jsonPath("$.formCode").value("liens"))
				.andExpect(jsonPath("$.formLabel").value("Liens"))
				.andExpect(jsonPath("$.prompt").value("Relie."))
				.andExpect(jsonPath("$.links[0].leftLabel").value("Pomme"))
				.andExpect(jsonPath("$.links[0].rightLabel").value("Fruit"))
				.andExpect(jsonPath("$.links[1].rightLabel").value("Animal"));
	}

	@Test
	void createGapsReturnsTheSentenceTheAnswerAndTheDecoy() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(questions.insert(eq(4L), eq("case_vide"), eq("Complète."), any()))
				.thenReturn(new StoredQuestion(
						12L,
						4L,
						"case_vide",
						"Complète.",
						new TeacherQuestionPort.StoredGaps(
								"Le chat mange une ___ dans le jardin.",
								List.of(new TeacherQuestionPort.StoredGapWord(1L, "pomme")),
								List.of(
										new TeacherQuestionPort.StoredGapWord(2L, "poire"),
										new TeacherQuestionPort.StoredGapWord(3L, "voiture")))));

		mockMvc.perform(post("/api/int/teacher/subjects/4/questions")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "formCode":"case_vide",
								  "prompt":"Complète.",
								  "sentence":"Le chat mange une ___ dans le jardin.",
								  "answers":[{"label":"pomme"}],
								  "decoys":[{"label":"poire"},{"label":"voiture"}]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(12))
				.andExpect(jsonPath("$.formCode").value("case_vide"))
				.andExpect(jsonPath("$.formLabel").value("Case vide"))
				.andExpect(jsonPath("$.prompt").value("Complète."))
				.andExpect(jsonPath("$.sentence").value("Le chat mange une ___ dans le jardin."))
				.andExpect(jsonPath("$.answers[0].label").value("pomme"))
				.andExpect(jsonPath("$.decoys[1].label").value("voiture"));
	}

	@Test
	void createFreeTextReturnsThePromptAndTheExpectedAnswer() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(questions.insert(eq(4L), eq("texte"), eq("Capitale de la France"), any()))
				.thenReturn(new StoredQuestion(
						13L,
						4L,
						"texte",
						"Capitale de la France",
						new TeacherQuestionPort.StoredFreeText("Paris")));

		mockMvc.perform(post("/api/int/teacher/subjects/4/questions")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "formCode":"texte",
								  "prompt":"Capitale de la France",
								  "expectedAnswer":"Paris"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(13))
				.andExpect(jsonPath("$.formCode").value("texte"))
				.andExpect(jsonPath("$.formLabel").value("Réponse libre"))
				.andExpect(jsonPath("$.prompt").value("Capitale de la France"))
				.andExpect(jsonPath("$.expectedAnswer").value("Paris"));
	}

	@Test
	void createFreeTextRefusesAnEmptyAnswer() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));

		mockMvc.perform(post("/api/int/teacher/subjects/4/questions")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "formCode":"texte",
								  "prompt":"Capitale de la France",
								  "expectedAnswer":"  "
								}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void createClickImageReturnsTheZonesAndTheRule() throws Exception {
		byte[] webp = Base64.getDecoder().decode("UklGRiQAAABXRUJQVlA4IBgAAAA=");
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(questions.insert(eq(4L), eq("image_interactive"), eq(""), any()))
				.thenReturn(new StoredQuestion(
						14L,
						4L,
						"image_interactive",
						"",
						new StoredClickImage(webp, true, List.of(new StoredClickZone(
								1L,
								"le soleil",
								true,
								"polygon",
								List.of(
										new ClickPoint(0.1, 0.2),
										new ClickPoint(0.3, 0.2),
										new ClickPoint(0.2, 0.4)))))));

		mockMvc.perform(post("/api/int/teacher/subjects/4/questions")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "formCode":"image_interactive",
								  "prompt":"",
								  "imageWebp":"UklGRiQAAABXRUJQVlA4IBgAAAA=",
								  "requireAllCorrect":true,
								  "zones":[{
								    "label":"le soleil",
								    "correct":true,
								    "shape":"polygon",
								    "points":[{"x":0.1,"y":0.2},{"x":0.3,"y":0.2},{"x":0.2,"y":0.4}]
								  }]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.formCode").value("image_interactive"))
				.andExpect(jsonPath("$.formLabel").value("Image à cliquer"))
				.andExpect(jsonPath("$.imageWebp").value("UklGRiQAAABXRUJQVlA4IBgAAAA="))
				.andExpect(jsonPath("$.requireAllCorrect").value(true))
				.andExpect(jsonPath("$.zones[0].label").value("le soleil"))
				.andExpect(jsonPath("$.zones[0].correct").value(true))
				.andExpect(jsonPath("$.zones[0].shape").value("polygon"))
				.andExpect(jsonPath("$.zones[0].points[0].x").value(0.1));
	}

	@Test
	void createPuzzleReturnsThePiecesWithoutACorrectSide() throws Exception {
		byte[] webp = Base64.getDecoder().decode("UklGRiQAAABXRUJQVlA4IBgAAAA=");
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(questions.insert(eq(4L), eq("puzzle"), eq(""), any()))
				.thenReturn(new StoredQuestion(
						15L,
						4L,
						"puzzle",
						"",
						new StoredPuzzle(webp, List.of(
								new StoredPuzzlePiece(1L, "le ciel", "rect", List.of(
										new ClickPoint(0.0, 0.0), new ClickPoint(1.0, 0.5))),
								new StoredPuzzlePiece(2L, "le sol", "rect", List.of(
										new ClickPoint(0.0, 0.5), new ClickPoint(1.0, 1.0)))))));

		mockMvc.perform(post("/api/int/teacher/subjects/4/questions")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "formCode":"puzzle",
								  "prompt":"",
								  "imageWebp":"UklGRiQAAABXRUJQVlA4IBgAAAA=",
								  "pieces":[
								    {
								      "label":"le ciel",
								      "shape":"rect",
								      "points":[{"x":0.0,"y":0.0},{"x":1.0,"y":0.5}]
								    },
								    {
								      "label":"le sol",
								      "shape":"rect",
								      "points":[{"x":0.0,"y":0.5},{"x":1.0,"y":1.0}]
								    }
								  ]
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.formCode").value("puzzle"))
				.andExpect(jsonPath("$.formLabel").value("Puzzle"))
				.andExpect(jsonPath("$.imageWebp").value("UklGRiQAAABXRUJQVlA4IBgAAAA="))
				.andExpect(jsonPath("$.requireAllCorrect").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.zones").isEmpty())
				.andExpect(jsonPath("$.pieces[0].label").value("le ciel"))
				.andExpect(jsonPath("$.pieces[0].shape").value("rect"))
				.andExpect(jsonPath("$.pieces[0].correct").doesNotExist())
				.andExpect(jsonPath("$.pieces[1].label").value("le sol"));
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
	void cloneReturnsTheCopiesAndKeepsTheSourcePath() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(subjects.findOwned(9L, 8L))
				.thenReturn(Optional.of(new StoredSubject(8L, 9L, 1L, "CE2", "Histoire", true)));
		when(questions.findInSubject(4L, 7L)).thenReturn(Optional.of(new StoredQuestion(
				7L,
				4L,
				"qcm",
				"Combien font 2 + 2 ?",
				List.of(new StoredChoice(1L, "3", false), new StoredChoice(2L, "4", true)),
				List.of(),
				List.of())));
		when(questions.insert(eq(8L), eq("qcm"), eq("Combien font 2 + 2 ?"), any()))
				.thenReturn(new StoredQuestion(
						20L,
						8L,
						"qcm",
						"Combien font 2 + 2 ?",
						List.of(new StoredChoice(3L, "3", false), new StoredChoice(4L, "4", true)),
						List.of(),
						List.of()));

		mockMvc.perform(post("/api/int/teacher/subjects/4/questions/clone")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"targetSubjectId":8,"questionIds":[7]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.questions.length()").value(1))
				.andExpect(jsonPath("$.questions[0].id").value(20))
				.andExpect(jsonPath("$.questions[0].prompt").value("Combien font 2 + 2 ?"))
				.andExpect(jsonPath("$.questions[0].choices[1].label").value("4"))
				.andExpect(jsonPath("$.questions[0].choices[1].correct").value(true));
	}

	@Test
	void cloneWithoutAQuestionIsRejected() throws Exception {
		mockMvc.perform(post("/api/int/teacher/subjects/4/questions/clone")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"targetSubjectId":8,"questionIds":[]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherQuestionService.CLONE_REQUIRED));
	}

	@Test
	void followersReturnsTheCountTheWarningAndTheNames() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(children.activeInClass(1L, "CE1")).thenReturn(List.of(new FollowingChild(3L, "Léa", "Martin")));
		when(children.linksOfSubject(4L)).thenReturn(List.of());

		mockMvc.perform(get("/api/int/teacher/subjects/4/followers")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.childCount").value(1))
				.andExpect(jsonPath("$.warning").value("1 enfant suit ce cours."))
				.andExpect(jsonPath("$.names[0]").value("Léa Martin"));
	}

	@Test
	void deleteReturnsTheRemovedIds() throws Exception {
		when(subjects.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true)));
		when(questions.findInSubject(4L, 7L)).thenReturn(Optional.of(new StoredQuestion(
				7L, 4L, "qcm", "2 + 2",
				List.of(new StoredChoice(1L, "4", true)), List.of(), List.of())));
		when(questions.findInSubject(4L, 8L)).thenReturn(Optional.of(new StoredQuestion(
				8L, 4L, "qcm", "3 + 3",
				List.of(new StoredChoice(2L, "6", true)), List.of(), List.of())));

		mockMvc.perform(post("/api/int/teacher/subjects/4/questions/delete")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"questionIds":[7,8]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deletedIds[0]").value(7))
				.andExpect(jsonPath("$.deletedIds[1]").value(8));
	}

	@Test
	void deleteWithoutAQuestionIsRejected() throws Exception {
		mockMvc.perform(post("/api/int/teacher/subjects/4/questions/delete")
						.header(HttpHeaders.AUTHORIZATION, bearer("teacher"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"questionIds":[]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(TeacherQuestionService.DELETE_REQUIRED));
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
