package com.appv4.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SCRUM-147 — vérifie le contrat et exporte {@code target/openapi.json} pour la CI.
 *
 * <p>Le fichier versionné {@code openapi/openapi.json} n'est jamais réécrit ici.
 * Pour l'actualiser après un vrai changement d'API : lancer les tests, puis
 * copier {@code target/openapi.json} vers {@code openapi/openapi.json} et committer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractExportTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void apiDocsDescribesAuthAndExportsJson() throws Exception {
		MvcResult result = mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andReturn();

		String body = result.getResponse().getContentAsString();
		assertThat(body)
				.contains("/api/int/auth/login")
				.contains("/api/int/auth/signup")
				.contains("/api/int/auth/oauth/google")
				.contains("/api/int/auth/oauth/facebook")
				.contains("/api/int/auth/oauth/apple")
				.contains("/api/int/auth/forgot-password")
				.contains("/api/int/auth/recover-identifier")
				.contains("recoverIdentifier")
				.contains("/api/int/auth/reset-password")
				.contains("/api/int/auth/me")
				.contains("/api/int/auth/recovery-prompt")
				.contains("answerRecoveryPrompt")
				.contains("/api/int/auth/spaces")
				.contains("/api/int/auth/spaces/parent")
				.contains("/api/int/auth/spaces/teacher")
				.contains("/api/int/admin/teacher-requests")
				.contains("/api/int/admin/humeurs")
				.contains("listHumeurChoices")
				.contains("/api/int/admin/app-hours")
				.contains("readAppHours")
				.contains("updateAppHours")
				.contains("/api/int/teacher/profile")
				.contains("/api/int/teacher/profile/removal-preview")
				.contains("/api/int/teacher/students")
				.contains("/api/int/teacher/students/{childId}/sejours")
				.contains("listTeacherSejours")
				.contains("/api/int/teacher/subjects")
				.contains("/api/int/teacher/question-forms")
				.contains("/api/int/teacher/subjects/{subjectId}/questions/clone")
				.contains("/api/int/teacher/subjects/{subjectId}/followers")
				.contains("/api/int/teacher/subjects/{subjectId}/questions/delete")
				.contains("/api/int/parent/profile")
				.contains("/api/int/parent/children")
				.contains("/api/int/parent/humeurs")
				.contains("listParentHumeurs")
				.contains("/api/int/parent/subjects")
				.contains("/api/ext/auth/login")
				.contains("/api/ext/auth/session")
				.contains("/api/ext/me")
				.contains("/api/ext/subjects")
				.contains("/api/ext/featured")
				.contains("readChildFeaturedQuestions")
				.contains("/api/ext/subjects/{subjectId}")
				.contains("/api/ext/subjects/{subjectId}/questions")
				.contains("/api/ext/subjects/{subjectId}/questions/{questionId}")
				.contains("/api/ext/subjects/{subjectId}/questions/{questionId}/attempts")
				.contains("/api/ext/progress")
				.contains("/api/ext/humeur")
				.contains("askChildHumeur")
				.contains("/api/ext/sejour/signals")
				.contains("signalChildSejour")
				.contains("/api/ext/sejour")
				.contains("readChildSejour")
				.contains("/api/int/parent/children/{childId}/sejours")
				.contains("listParentSejours")
				.contains("/api/ext/avatar")
				.contains("readChildAvatar")
				.contains("saveChildAvatarStyle")
				.contains("/api/ext/today");
		assertThat(body).doesNotContain("/api/ext/hello", "/api/int/hello",
				"/api/int/parent/hello", "/api/int/teacher/hello");
		// switch-role est authentifié — le contrat doit l'annoncer (issue #51)
		int switchRoleIdx = body.indexOf("\"operationId\":\"switchRole\"");
		assertThat(switchRoleIdx).isPositive();
		String switchRoleSlice = body.substring(switchRoleIdx, Math.min(body.length(), switchRoleIdx + 500));
		assertThat(switchRoleSlice).contains("\"security\":[{\"bearer-jwt\":[]}]");

		JsonNode contract = new ObjectMapper().readTree(body);
		assertClosedMark(contract, "ChildAttemptResultResponse");
		assertClosedMark(contract, "ChildLearningQuestionItem");
		assertClosedMark(contract, "ChildSubjectLessonQuestion");
		for (JsonNode variant : contract.at("/components/schemas/SubmitChildAttemptRequest/oneOf")) {
			String ref = variant.path("$ref").asText();
			String schemaName = ref.substring(ref.lastIndexOf('/') + 1);
			boolean playIdRequired = false;
			for (JsonNode field : contract.at("/components/schemas/" + schemaName + "/required")) {
				playIdRequired = playIdRequired || "playId".equals(field.asText());
			}
			assertThat(playIdRequired).as(schemaName).isTrue();
		}

		Path targetOut = Path.of("target", "openapi.json");
		Files.createDirectories(targetOut.getParent());
		Files.writeString(targetOut, body);
		assertThat(targetOut).exists().isRegularFile();

		Path repoOut = Path.of("openapi", "openapi.json");
		assertThat(repoOut)
				.as("contrat versionné attendu pour le front (SCRUM-148)")
				.exists()
				.isRegularFile();
		String committed = Files.readString(repoOut);
		assertThat(body)
				.as("contrat généré dérivé de openapi/openapi.json — "
						+ "copie target/openapi.json vers openapi/openapi.json après revue")
				.isEqualTo(committed);
	}

	private static void assertClosedMark(JsonNode contract, String schemaName) {
		JsonNode mark = contract.at("/components/schemas/" + schemaName + "/properties/mark");
		if (mark.has("$ref")) {
			String ref = mark.get("$ref").asText();
			mark = contract.at("/components/schemas/" + ref.substring(ref.lastIndexOf('/') + 1));
		}
		assertThat(mark.path("enum").toString())
				.as(schemaName)
				.isEqualTo("[\"none\",\"black\",\"yellow\"]");
	}
}
