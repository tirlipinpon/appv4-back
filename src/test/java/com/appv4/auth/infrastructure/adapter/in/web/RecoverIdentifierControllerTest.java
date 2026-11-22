package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.port.out.RecoveryAccountPort;
import com.appv4.auth.application.port.out.RecoveryAccountPort.RecoveryCompte;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RecoverIdentifierControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private RecoveryAccountPort recoveryAccountPort;

	@Test
	void publicRouteReturnsTheMaskAndNotTheLoginEmail() throws Exception {
		when(recoveryAccountPort.all()).thenReturn(List.of(new RecoveryCompte(
				7L,
				"marie@ecole.be",
				"elodie martin",
				"+32 470 12 34 56",
				"Marie@ecole.be",
				true)));

		mockMvc.perform(post("/api/int/auth/recover-identifier")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"displayName":"Elodi Martin","phone":"0470 12 34 56",
								"contactEmail":"marue@ecole.be"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.maskedEmail").value("ma***@ecole.be"))
				.andExpect(jsonPath("$.message").doesNotExist());
	}
}
