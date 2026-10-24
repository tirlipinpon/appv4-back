package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.port.out.AppleOAuthClient;
import com.appv4.auth.application.port.out.AppleOAuthClient.AppleUserProfile;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.AuthUserAccountPort.SessionState;
import com.appv4.auth.infrastructure.config.AppleOAuthProperties;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppleOAuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AppleOAuthProperties appleOAuthProperties;

	@MockBean
	private AppleOAuthClient appleOAuthClient;

	@MockBean
	private AuthUserAccountPort authUserAccountPort;

	@BeforeEach
	void resetAppleProps() {
		appleOAuthProperties.setClientId("");
		appleOAuthProperties.setTeamId("");
		appleOAuthProperties.setKeyId("");
		appleOAuthProperties.setPrivateKey("");
		when(authUserAccountPort.findSessionState(anyLong()))
				.thenReturn(Optional.of(new SessionState(true, 0)));
	}

	@Test
	void authorizeUrlRequiresConfig() throws Exception {
		mockMvc.perform(get("/api/int/auth/oauth/apple/authorize-url")
						.param("redirectUri", "http://localhost:4201/auth/callback/apple"))
				.andExpect(status().isServiceUnavailable());
	}

	@Test
	void authorizeUrlReturnsAppleUrlWhenConfigured() throws Exception {
		appleOAuthProperties.setClientId("com.appv4.service");
		appleOAuthProperties.setTeamId("TEAM123");
		appleOAuthProperties.setKeyId("KEY123");
		appleOAuthProperties.setPrivateKey("dummy");

		mockMvc.perform(get("/api/int/auth/oauth/apple/authorize-url")
						.param("redirectUri", "http://localhost:4201/auth/callback/apple"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authorizationUrl")
						.value(org.hamcrest.Matchers.containsString("appleid.apple.com")))
				.andExpect(jsonPath("$.authorizationUrl")
						.value(org.hamcrest.Matchers.containsString("com.appv4.service")));
	}

	@Test
	void loginWithCodeReturnsJwt() throws Exception {
		when(appleOAuthClient.exchangeCode(eq("auth-code"), eq("http://localhost:4201/cb")))
				.thenReturn(new AppleUserProfile("apple-1", "user@example.com", ""));
		when(authUserAccountPort.findByProviderSubject("apple", "apple-1"))
				.thenReturn(Optional.of(new AuthUserAccount(7L, "user@example.com", "User", true)));

		mockMvc.perform(post("/api/int/auth/oauth/apple")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"authorizationCode":"auth-code","redirectUri":"http://localhost:4201/cb"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.expiresAtEpochSeconds").isNumber());
	}
}
