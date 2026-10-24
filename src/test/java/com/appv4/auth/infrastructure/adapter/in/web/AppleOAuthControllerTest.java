package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.port.out.AppleOAuthClient;
import com.appv4.auth.application.port.out.AppleOAuthClient.AppleUserProfile;
import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.AuthUserAccountPort.SessionState;
import com.appv4.auth.application.service.AppleOAuthReturnUris;
import com.appv4.auth.infrastructure.config.AppleOAuthProperties;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppleOAuthControllerTest {

	private static final String CALLBACK = "https://api.example.com/api/int/auth/oauth/apple/callback";

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
		appleOAuthProperties.setCallbackUri("");
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
	void authorizeUrlReturnsFormPostWhenConfigured() throws Exception {
		appleOAuthProperties.setClientId("com.appv4.service");
		appleOAuthProperties.setTeamId("TEAM123");
		appleOAuthProperties.setKeyId("KEY123");
		appleOAuthProperties.setPrivateKey("dummy");
		appleOAuthProperties.setCallbackUri(CALLBACK);

		mockMvc.perform(get("/api/int/auth/oauth/apple/authorize-url")
						.param("redirectUri", "http://localhost:4201/auth/callback/apple"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authorizationUrl")
						.value(org.hamcrest.Matchers.containsString("response_mode=form_post")))
				.andExpect(jsonPath("$.authorizationUrl")
						.value(org.hamcrest.Matchers.containsString(
								java.net.URLEncoder.encode(
										CALLBACK, java.nio.charset.StandardCharsets.UTF_8))));
	}

	@Test
	void formPostCallbackRedirectsToSpa() throws Exception {
		String state = AppleOAuthReturnUris.encodeState("http://localhost:4201/auth/callback/apple");

		mockMvc.perform(post("/api/int/auth/oauth/apple/callback")
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("state", state)
						.param("code", "auth-code"))
				.andExpect(status().isFound())
				.andExpect(header().string(
						"Location",
						"http://localhost:4201/auth/callback/apple?code=auth-code"));
	}

	@Test
	void loginWithCodeUsesConfiguredCallbackUri() throws Exception {
		appleOAuthProperties.setCallbackUri(CALLBACK);
		when(appleOAuthClient.exchangeCode(eq("auth-code"), eq(CALLBACK)))
				.thenReturn(new AppleUserProfile("apple-1", "user@example.com", ""));
		when(authUserAccountPort.findByProviderSubject("apple", "apple-1"))
				.thenReturn(Optional.of(new AuthUserAccount(7L, "user@example.com", "User", true)));

		mockMvc.perform(post("/api/int/auth/oauth/apple")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"authorizationCode":"auth-code","redirectUri":"http://localhost:4201/ignored"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.expiresAtEpochSeconds").isNumber());
	}
}
