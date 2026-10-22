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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * SCRUM-147 — vérifie le contrat et exporte {@code target/openapi.json} pour la CI / le front.
 */
@SpringBootTest
@AutoConfigureMockMvc
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
				.contains("/api/int/hello")
				.contains("/api/ext/hello");

		// Artefact CI (target/) + contrat versionné pour le front (openapi/)
		Path targetOut = Path.of("target", "openapi.json");
		Path repoOut = Path.of("openapi", "openapi.json");
		Files.createDirectories(targetOut.getParent());
		Files.createDirectories(repoOut.getParent());
		Files.writeString(targetOut, body);
		Files.writeString(repoOut, body);
		assertThat(targetOut).exists().isRegularFile();
		assertThat(repoOut).exists().isRegularFile();
	}
}
