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
	void apiDocsDescribesAuthAndHelloAndExportsJson() throws Exception {
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
				.contains("/api/int/auth/reset-password")
				.contains("/api/int/auth/me")
				.contains("/api/int/auth/spaces")
				.contains("/api/int/auth/spaces/parent")
				.contains("/api/int/auth/spaces/teacher")
				.contains("/api/int/admin/teacher-requests")
				.contains("/api/int/teacher/profile")
				.contains("/api/int/teacher/students")
				.contains("/api/int/teacher/subjects")
				.contains("/api/int/teacher/question-forms")
				.contains("/api/int/teacher/subjects/{subjectId}/questions/clone")
				.contains("/api/int/parent/children")
				.contains("/api/int/parent/subjects")
				.contains("/api/int/hello")
				.contains("/api/ext/hello");
		// switch-role est authentifié — le contrat doit l'annoncer (issue #51)
		int switchRoleIdx = body.indexOf("\"operationId\":\"switchRole\"");
		assertThat(switchRoleIdx).isPositive();
		String switchRoleSlice = body.substring(switchRoleIdx, Math.min(body.length(), switchRoleIdx + 500));
		assertThat(switchRoleSlice).contains("\"security\":[{\"bearer-jwt\":[]}]");

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
}
