package com.appv4.auth.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
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
import com.appv4.auth.application.port.out.ChildSimonChallengePort;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.NewAttempt;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.StoredAttempt;
import com.appv4.auth.application.port.out.ChildSessionTokenPort;
import com.appv4.auth.application.port.out.ChildSubjectPort;
import com.appv4.auth.application.port.out.ChildSubjectPort.CatalogSubject;
import com.appv4.auth.application.port.out.ChildSubjectPort.StoredLink;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.ClickPoint;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredCard;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoice;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredChoices;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClassification;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickImage;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredClickZone;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredColumn;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredContent;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredGapWord;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredGaps;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredItem;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredLinks;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredItems;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPair;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPairs;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredFreeText;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPuzzle;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredPuzzlePiece;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredSimon;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredSimonElement;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredStatement;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredStatements;
import com.appv4.auth.application.port.out.TeacherSubjectPort;
import com.appv4.auth.application.port.out.TeacherSubjectPort.StoredSubject;
import com.appv4.auth.application.service.ChildQuestionPlayService;
import com.appv4.auth.application.service.question.play.FreeTextChildAnswerGrader;
import com.appv4.auth.infrastructure.adapter.out.jwt.JjwtTokenAdapter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChildQuestionPlayControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper json;

	@Autowired
	private ChildSessionTokenPort childTokens;

	@Autowired
	private JjwtTokenAdapter adultTokens;

	@MockBean
	private ChildLoginPort childLoginPort;

	@MockBean
	private ChildSubjectPort childSubjectPort;

	@MockBean
	private TeacherQuestionPort teacherQuestionPort;

	@MockBean
	private TeacherSubjectPort teacherSubjectPort;

	@MockBean
	private ChildQuestionAttemptPort childQuestionAttemptPort;

	@MockBean
	private ChildSimonChallengePort childSimonChallengePort;

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
				.andExpect(jsonPath("$.formLabel").value("Vrai ou faux"))
				.andExpect(jsonPath("$.iconCode").value("true-false"))
				.andExpect(jsonPath("$.color").value("#2bb673"))
				.andExpect(jsonPath("$.statements[0].statementId").value(1))
				.andExpect(jsonPath("$.statements[0].label").value("2+2=4"))
				.andExpect(jsonPath("$.statements[0].truthful").doesNotExist())
				.andExpect(jsonPath("$.statements[1].statementId").value(2));
	}

	@Test
	void structuredPlayExposesOnlyTheContentOfTheForm() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 19L)).thenReturn(Optional.of(freeTextQuestion()));

		mockMvc.perform(post("/api/ext/subjects/4/questions/19/play").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.formCode").value("texte"))
				.andExpect(jsonPath("$.content").isMap())
				.andExpect(jsonPath("$.content.expectedAnswer").doesNotExist())
				.andExpect(jsonPath("$.choices").doesNotExist());
	}

	@Test
	void submitRejectsMissingValuesAndMixedVariants() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		for (String body : List.of(
				"{\"formCode\":\"vrai_faux\",\"statements\":[{\"statementId\":1}]}",
				"{\"formCode\":\"vrai_faux\",\"statements\":[null]}",
				"{\"formCode\":\"puzzle\",\"puzzlePlacements\":[{\"pieceId\":1,\"y\":0}]}",
				"{\"formCode\":\"case_vide\",\"gapFills\":[{\"wordId\":1}]}",
				"{\"formCode\":\"vrai_faux\",\"statements\":[{\"statementId\":1,\"value\":false}],\"attempt\":\"mot\"}")) {
			mockMvc.perform(post("/api/ext/subjects/4/questions/11/attempts")
							.header("Authorization", "Bearer " + token)
							.contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("playContracts")
	void modernChildAndTeacherContractsAgreeForEachForm(PlayContract form) throws Exception {
		long questionId = 19L;
		String childAuth = "Bearer " + childTokens.issue(7L, "Léa").token();
		String teacherAuth = "Bearer " + adultTokens
				.encode(9L, "prof@example.com", "teacher", List.of("teacher"), false, 0).token();
		String childUrl = "/api/ext/subjects/4/questions/" + questionId;
		String previewUrl = "/api/int/teacher/subjects/4/questions/" + questionId + "/preview";
		stubLinkedChild();
		when(teacherSubjectPort.findOwned(9L, 4L))
				.thenReturn(Optional.of(new StoredSubject(4L, 9L, 1L, "CE1", "Mathématiques", true, null)));
		when(teacherQuestionPort.findInSubject(4L, questionId)).thenReturn(Optional.of(
				new StoredQuestion(questionId, 4L, form.code(), "Question", form.stored())));

		JsonNode preview = json.readTree(mockMvc.perform(get(previewUrl).header("Authorization", teacherAuth))
				.andExpect(status().isOk()).andExpect(jsonPath("$.formCode").value(form.code()))
				.andExpect(jsonPath("$.content").isMap()).andExpect(jsonPath("$.challengeId").doesNotExist())
				.andReturn().getResponse().getContentAsByteArray()).get("content");
		ObjectNode envelope = json.createObjectNode();
		envelope.set("answer", playAnswer(form, preview));
		if (form.simon()) {
			envelope.set("previewSequence", preview.get("sequence"));
		}
		mockMvc.perform(post(previewUrl + "/attempts").header("Authorization", teacherAuth)
						.contentType(MediaType.APPLICATION_JSON).content(envelope.toString()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.scorePercent").value(form.score()))
				.andExpect(jsonPath("$.passed").value(form.score() == 100))
				.andExpect(jsonPath("$.attemptId").doesNotExist());
		verifyNoInteractions(childQuestionAttemptPort, childSimonChallengePort);

		UUID challengeId = UUID.randomUUID();
		if (form.simon()) {
			when(childSimonChallengePort.issue(eq(7L), eq(questionId), anyList(), eq(false))).thenReturn(challengeId);
		}
		MvcResult opened = mockMvc.perform(post(childUrl + "/play").header("Authorization", childAuth))
				.andExpect(status().isOk()).andExpect(jsonPath("$.formCode").value(form.code()))
				.andExpect(jsonPath("$.content").isMap()).andReturn();
		JsonNode question = json.readTree(opened.getResponse().getContentAsByteArray());
		ObjectNode answer = playAnswer(form, question.get("content"));
		if (form.simon()) {
			List<Integer> sequence = JsonPath.read(opened.getResponse().getContentAsString(), "$.content.sequence");
			when(childSimonChallengePort.take(7L, questionId, challengeId)).thenReturn(Optional.of(sequence));
			mockMvc.perform(post(childUrl + "/attempts").header("Authorization", childAuth)
							.contentType(MediaType.APPLICATION_JSON).content(answer.toString()))
					.andExpect(status().isBadRequest());
			answer.put("challengeId", question.get("challengeId").asText());
		}

		ObjectNode missingAnswer = answer.deepCopy();
		missingAnswer.remove(form.answerField());
		mockMvc.perform(post(childUrl + "/attempts").header("Authorization", childAuth)
						.contentType(MediaType.APPLICATION_JSON).content(missingAnswer.toString()))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
		verifyNoInteractions(childQuestionAttemptPort);
		when(childQuestionAttemptPort.insert(any(NewAttempt.class))).thenAnswer(call -> {
			NewAttempt attempt = call.getArgument(0);
			return new StoredAttempt(42L, attempt.childId(), attempt.subjectId(), attempt.questionId(),
					attempt.formCode(), attempt.scorePercent(), attempt.passed());
		});
		mockMvc.perform(post(childUrl + "/attempts").header("Authorization", childAuth)
						.contentType(MediaType.APPLICATION_JSON).content(answer.toString()))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.scorePercent").value(form.score()))
				.andExpect(jsonPath("$.passed").value(form.score() == 100));
	}

	private ObjectNode playAnswer(PlayContract form, JsonNode content) throws Exception {
		ObjectNode answer = json.createObjectNode().put("formCode", form.code());
		switch (form.code()) {
			case "memory" -> answer.putArray(form.answerField()).addObject()
					.put("firstCardId", content.path("memoryCards").get(0).path("cardId").asText())
					.put("secondCardId", content.path("memoryCards").get(1).path("cardId").asText());
			case "liens" -> {
				var matches = answer.putArray(form.answerField());
				for (int index = 0; index < 2; index++) {
					JsonNode left = content.path("linkLefts").get(index);
					JsonNode right = content.path("linkRights").get(1 - index);
					matches.addObject().put("leftId", left.path("optionId").asText())
							.put("rightId", right.path("optionId").asText());
				}
			}
			case "simon" -> answer.putArray(form.answerField()).add(content.path("sequence").get(0).asInt());
			default -> answer.set(form.answerField(), json.readTree(form.value()));
		}
		return answer;
	}

	private static Stream<PlayContract> playContracts() {
		List<ClickPoint> rectangle = List.of(new ClickPoint(0, 0), new ClickPoint(.2, .2));
		return Stream.of(
				new PlayContract("qcm", new StoredChoices(List.of(new StoredChoice(1L, "4", true))),
						"selectedChoiceIds", "[1]", 100),
				new PlayContract("vrai_faux", new StoredStatements(List.of(new StoredStatement(1L, "2+2=5", false))),
						"statements", "[{\"statementId\":1,\"value\":false}]", 100),
				new PlayContract("memory", new StoredPairs(List.of(new StoredPair(1L, "chat", "cat"))),
						"memoryMatches", null, 100),
				new PlayContract("order", new StoredItems(List.of(new StoredItem(1L, "A"), new StoredItem(2L, "B"))),
						"orderedItemIds", "[1,2]", 100),
				new PlayContract("classement", new StoredClassification(
						List.of(new StoredColumn(2L, "Fruits")), List.of(new StoredCard(1L, "Pomme", 2L))),
						"placements", "[{\"cardId\":1,\"columnId\":2}]", 100),
				new PlayContract("liens", new StoredLinks(List.of(
						new TeacherQuestionPort.StoredLink(1L, "chat", "cat"),
						new TeacherQuestionPort.StoredLink(2L, "chat", "Katze"))),
						"linkSelections", null, 100),
				new PlayContract("case_vide", new StoredGaps("___ et ___", List.of(
						new StoredGapWord(1L, "chat"), new StoredGapWord(2L, "chat")), List.of()),
						"gapFills", "[{\"holeIndex\":0,\"wordId\":2},{\"holeIndex\":1,\"wordId\":1}]", 100),
				new PlayContract("image_interactive", new StoredClickImage(new byte[]{1}, true, false, List.of(
						new StoredClickZone(1L, "A", true, "rect", rectangle, null),
						new StoredClickZone(2L, "B", true, "rect", rectangle, null))),
						"zoneClicks", "[{\"zoneId\":1}]", 50),
				new PlayContract("reponse_libre", new StoredFreeText("chat"), "attempt", "\"char\"", 75),
				new PlayContract("texte", new StoredFreeText("Paris"), "attempt", "\"\u00a0Pâris\u00a0\"", 100),
				new PlayContract("puzzle", new StoredPuzzle(new byte[]{1}, "blanc", null, .08, List.of(
						new StoredPuzzlePiece(1L, "A", "rect", rectangle, 0.0, 0.0, null))),
						"puzzlePlacements", "[{\"pieceId\":1,\"x\":-0.02,\"y\":0}]", 100),
				new PlayContract("simon", simonQuestion().content(), "simonSequence", null, 25));
	}

	private record PlayContract(String code, StoredContent stored, String answerField, String value, int score) {
		boolean simon() {
			return "simon".equals(code);
		}

		@Override
		public String toString() {
			return code;
		}
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
	void getLinksReturnsBothSides() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 15L)).thenReturn(Optional.of(new StoredQuestion(
				15L,
				4L,
				"liens",
				"Relie.",
				new StoredLinks(List.of(
						new TeacherQuestionPort.StoredLink(1L, "Pomme", "Fruit"),
						new TeacherQuestionPort.StoredLink(2L, "Chien", "Animal"))))));

		mockMvc.perform(get("/api/ext/subjects/4/questions/15").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.formCode").value("liens"))
				.andExpect(jsonPath("$.links.length()").value(2))
				.andExpect(jsonPath("$.links[0].leftLabel").value("Pomme"))
				.andExpect(jsonPath("$.links[0].rightLabel").value("Fruit"))
				.andExpect(jsonPath("$.links[1].linkId").value(2));
	}

	@Test
	void submitLinksAllCorrectPasses() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 15L)).thenReturn(Optional.of(new StoredQuestion(
				15L,
				4L,
				"liens",
				"Relie.",
				new StoredLinks(List.of(
						new TeacherQuestionPort.StoredLink(1L, "Pomme", "Fruit"),
						new TeacherQuestionPort.StoredLink(2L, "Chien", "Animal"))))));
		when(childQuestionAttemptPort.insert(any(NewAttempt.class)))
				.thenReturn(new StoredAttempt(46L, 7L, 4L, 15L, "liens", 100, true));

		mockMvc.perform(post("/api/ext/subjects/4/questions/15/attempts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"linkMatches":[{"leftId":1,"rightId":1},{"leftId":2,"rightId":2}]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.attemptId").value(46))
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
	void getGapsReturnsSentenceAndBank() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 16L)).thenReturn(Optional.of(new StoredQuestion(
				16L,
				4L,
				"case_vide",
				"Complète.",
				new TeacherQuestionPort.StoredGaps(
						"Le ___ mange une ___.",
						List.of(
								new TeacherQuestionPort.StoredGapWord(10L, "chat"),
								new TeacherQuestionPort.StoredGapWord(11L, "pomme")),
						List.of(new TeacherQuestionPort.StoredGapWord(12L, "voiture"))))));

		mockMvc.perform(get("/api/ext/subjects/4/questions/16").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.formCode").value("case_vide"))
				.andExpect(jsonPath("$.sentence").value("Le ___ mange une ___."))
				.andExpect(jsonPath("$.words.length()").value(3))
				.andExpect(jsonPath("$.words[?(@.wordId == 10)].label").value("chat"))
				.andExpect(jsonPath("$.words[?(@.wordId == 12)].label").value("voiture"));
	}

	@Test
	void submitGapsAllCorrectPasses() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 16L)).thenReturn(Optional.of(new StoredQuestion(
				16L,
				4L,
				"case_vide",
				"Complète.",
				new TeacherQuestionPort.StoredGaps(
						"Le ___ mange une ___.",
						List.of(
								new TeacherQuestionPort.StoredGapWord(10L, "chat"),
								new TeacherQuestionPort.StoredGapWord(11L, "pomme")),
						List.of(new TeacherQuestionPort.StoredGapWord(12L, "voiture"))))));
		when(childQuestionAttemptPort.insert(any(NewAttempt.class)))
				.thenReturn(new StoredAttempt(47L, 7L, 4L, 16L, "case_vide", 100, true));

		mockMvc.perform(post("/api/ext/subjects/4/questions/16/attempts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"gapFills":[{"holeIndex":0,"wordId":10},{"holeIndex":1,"wordId":11}]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.attemptId").value(47))
				.andExpect(jsonPath("$.scorePercent").value(100))
				.andExpect(jsonPath("$.passed").value(true));
	}

	@Test
	void getSimonReturnsTheElementsInOrderAndTheSequenceToShow() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 18L)).thenReturn(Optional.of(simonQuestion()));

		mockMvc.perform(get("/api/ext/subjects/4/questions/18").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.formCode").value("simon"))
				.andExpect(jsonPath("$.elementType").value("couleurs"))
				.andExpect(jsonPath("$.elements.length()").value(4))
				.andExpect(jsonPath("$.elements[0].label").value("rouge"))
				.andExpect(jsonPath("$.elements[0].color").value("#ff0000"))
				.andExpect(jsonPath("$.elements[3].label").value("jaune"))
				.andExpect(jsonPath("$.sequence.length()").value(4));
	}

	@Test
	void submitSimonSameSequencePassesAndStoresOneLinePerStep() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 18L)).thenReturn(Optional.of(simonQuestion()));
		when(childSimonChallengePort.take(7L, 18L, null)).thenReturn(Optional.of(List.of(0, 1, 2, 0)));
		when(childQuestionAttemptPort.insert(any(NewAttempt.class)))
				.thenReturn(new StoredAttempt(48L, 7L, 4L, 18L, "simon", 100, true));

		mockMvc.perform(post("/api/ext/subjects/4/questions/18/attempts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"simonSequence":[0,1,2,0]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.attemptId").value(48))
				.andExpect(jsonPath("$.scorePercent").value(100))
				.andExpect(jsonPath("$.passed").value(true));

		ArgumentCaptor<NewAttempt> attempt = ArgumentCaptor.forClass(NewAttempt.class);
		verify(childQuestionAttemptPort).insert(attempt.capture());
		assertThat(attempt.getValue().formCode()).isEqualTo("simon");
		assertThat(attempt.getValue().scorePercent()).isEqualTo(100);
		assertThat(attempt.getValue().answers()).hasSize(4)
				.allMatch(line -> "simon_step".equals(line.targetKind()) && line.correct());
	}

	@Test
	void submitSimonInAnotherOrderIsZero() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 18L)).thenReturn(Optional.of(simonQuestion()));
		when(childSimonChallengePort.take(7L, 18L, null)).thenReturn(Optional.of(List.of(0, 1, 2, 0)));
		when(childQuestionAttemptPort.insert(any(NewAttempt.class)))
				.thenReturn(new StoredAttempt(49L, 7L, 4L, 18L, "simon", 25, false));

		mockMvc.perform(post("/api/ext/subjects/4/questions/18/attempts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"simonSequence":[0,2,1,0]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.scorePercent").value(25))
				.andExpect(jsonPath("$.passed").value(false));

		ArgumentCaptor<NewAttempt> attempt = ArgumentCaptor.forClass(NewAttempt.class);
		verify(childQuestionAttemptPort).insert(attempt.capture());
		assertThat(attempt.getValue().scorePercent()).isEqualTo(25);
		assertThat(attempt.getValue().passed()).isFalse();
	}

	@Test
	void submitSimonWithAMissingStepIsRefused() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 18L)).thenReturn(Optional.of(simonQuestion()));
		when(childSimonChallengePort.take(7L, 18L, null)).thenReturn(Optional.of(List.of(0, 1, 2, 0)));

		mockMvc.perform(post("/api/ext/subjects/4/questions/18/attempts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"simonSequence":[0,null,2,0]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BAD_REQUEST"));

		verify(childQuestionAttemptPort, never()).insert(any(NewAttempt.class));
	}

	@Test
	void getFreeTextHidesExpectedAnswer() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 19L)).thenReturn(Optional.of(freeTextQuestion()));

		mockMvc.perform(get("/api/ext/subjects/4/questions/19").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.formCode").value("texte"))
				.andExpect(jsonPath("$.prompt").value("Capitale de la France"))
				.andExpect(jsonPath("$.expectedAnswer").doesNotExist())
				.andExpect(content().string(not(containsString("Paris"))));
	}

	@Test
	void submitFreeTextReportsThePageBudget() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		UUID page = UUID.fromString("11111111-1111-1111-1111-111111111111");
		when(teacherQuestionPort.findInSubject(4L, 19L)).thenReturn(Optional.of(freeTextQuestion()));
		when(teacherQuestionPort.gamificationOf(19L)).thenReturn(
				new com.appv4.auth.application.port.out.TeacherQuestionPort.StoredGamification(
						2, "modere", true, false, List.of(20, 10)));
		when(childQuestionAttemptPort.claim(page, 7L, 4L, 19L, 2))
				.thenReturn(Optional.of(new com.appv4.auth.application.port.out.ChildQuestionAttemptPort.PageClaim(2, 0)));
		when(childQuestionAttemptPort.insert(any(NewAttempt.class)))
				.thenReturn(new StoredAttempt(51L, 7L, 4L, 19L, "texte", 100, true));

		mockMvc.perform(post("/api/ext/subjects/4/questions/19/attempts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"attempt":"Paris","playId":"11111111-1111-1111-1111-111111111111"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.attemptId").value(51))
				.andExpect(jsonPath("$.scorePercent").value(100))
				.andExpect(jsonPath("$.passed").value(true))
				.andExpect(jsonPath("$.attemptRank").value(2))
				.andExpect(jsonPath("$.attemptsRemaining").value(0))
				.andExpect(jsonPath("$.pointsAwarded").value(10))
				.andExpect(jsonPath("$.replayAvailable").value(false));
	}

	@Test
	void openTellsHowManyAttemptsThePageStillHas() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 19L)).thenReturn(Optional.of(freeTextQuestion()));
		when(childQuestionAttemptPort.attemptsUsed(
				UUID.fromString("11111111-1111-1111-1111-111111111111"), 7L, 4L, 19L)).thenReturn(1);
		when(teacherQuestionPort.gamificationOf(19L)).thenReturn(
				new com.appv4.auth.application.port.out.TeacherQuestionPort.StoredGamification(
						2, "facile", false, false, List.of()));

		mockMvc.perform(post("/api/ext/subjects/4/questions/19/play")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"playId":"11111111-1111-1111-1111-111111111111"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.maxAttempts").value(2))
				.andExpect(jsonPath("$.attemptsRemaining").value(1))
				.andExpect(jsonPath("$.formCode").value("texte"));
	}

	@Test
	void submitFreeTextAcceptsAccentAndCase() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 19L)).thenReturn(Optional.of(freeTextQuestion()));
		when(childQuestionAttemptPort.insert(any(NewAttempt.class)))
				.thenReturn(new StoredAttempt(49L, 7L, 4L, 19L, "texte", 100, true));

		mockMvc.perform(post("/api/ext/subjects/4/questions/19/attempts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"attempt":"  Pâris  "}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.attemptId").value(49))
				.andExpect(jsonPath("$.scorePercent").value(100))
				.andExpect(jsonPath("$.passed").value(true));

		ArgumentCaptor<NewAttempt> attempt = ArgumentCaptor.forClass(NewAttempt.class);
		verify(childQuestionAttemptPort).insert(attempt.capture());
		assertThat(attempt.getValue().formCode()).isEqualTo("texte");
		assertThat(attempt.getValue().passed()).isTrue();
		assertThat(attempt.getValue().answers()).hasSize(1);
		assertThat(attempt.getValue().answers().get(0).targetKind()).isEqualTo("texte");
	}

	@Test
	void submitFreeTextWrongAnswerIsNotPassed() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 19L)).thenReturn(Optional.of(freeTextQuestion()));
		when(childQuestionAttemptPort.insert(any(NewAttempt.class)))
				.thenReturn(new StoredAttempt(50L, 7L, 4L, 19L, "texte", 0, false));

		mockMvc.perform(post("/api/ext/subjects/4/questions/19/attempts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"attempt":"Lyon"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.scorePercent").value(0))
				.andExpect(jsonPath("$.passed").value(false));
	}

	@Test
	void submitBlankFreeTextIsRefused() throws Exception {
		String token = loginAsLea();
		stubLinkedChild();
		when(teacherQuestionPort.findInSubject(4L, 19L)).thenReturn(Optional.of(freeTextQuestion()));

		mockMvc.perform(post("/api/ext/subjects/4/questions/19/attempts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"attempt":"   "}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(FreeTextChildAnswerGrader.BAD_ANSWER));

		verify(childQuestionAttemptPort, never()).insert(any(NewAttempt.class));
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

	private static StoredQuestion freeTextQuestion() {
		return new StoredQuestion(19L, 4L, "texte", "Capitale de la France", new StoredFreeText("Paris"));
	}

	/** Rouge, bleu, vert, jaune ; suite rouge, bleu, vert, rouge. */
	private static StoredQuestion simonQuestion() {
		return new StoredQuestion(18L, 4L, "simon", "", new StoredSimon("couleurs", List.of(
				new StoredSimonElement(1L, "rouge", "#ff0000"),
				new StoredSimonElement(2L, "bleu", "#0000ff"),
				new StoredSimonElement(3L, "vert", "#00ff00"),
				new StoredSimonElement(4L, "jaune", "#ffff00"))));
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
