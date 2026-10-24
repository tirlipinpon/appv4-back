package com.appv4.auth.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.AuthUserAccountPort.SessionState;
import com.appv4.auth.application.port.out.GoogleOAuthClient;
import com.appv4.auth.application.port.out.GoogleOAuthClient.GoogleUserProfile;
import com.appv4.auth.application.port.out.OAuthBrowserBindingPort;
import com.appv4.auth.infrastructure.config.GoogleOAuthProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleOAuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private GoogleOAuthProperties googleOAuthProperties;

	@Autowired
	private OAuthBrowserBindingPort oauthBrowserBindingPort;

	@MockBean
	private GoogleOAuthClient googleOAuthClient;

	@MockBean
	private AuthUserAccountPort authUserAccountPort;

	@BeforeEach
	void resetGoogleProps() {
		googleOAuthProperties.setClientId("");
		googleOAuthProperties.setClientSecret("");
		when(authUserAccountPort.findSessionState(anyLong()))
				.thenReturn(java.util.Optional.of(new SessionState(true, 0)));
	}

	@Test
	void authorizeUrlRequiresConfig() throws Exception {
		mockMvc.perform(get("/api/int/auth/oauth/google/authorize-url")
						.param("redirectUri", "http://localhost:4201/auth/callback/google"))
				.andExpect(status().isServiceUnavailable());
	}

	@Test
	void authorizeUrlReturnsGoogleUrlWithStateWhenConfigured() throws Exception {
		googleOAuthProperties.setClientId("client-abc");
		googleOAuthProperties.setClientSecret("secret-xyz");

		mockMvc.perform(get("/api/int/auth/oauth/google/authorize-url")
						.param("redirectUri", "http://localhost:4201/auth/callback/google"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authorizationUrl")
						.value(org.hamcrest.Matchers.containsString("client-abc")))
				.andExpect(jsonPath("$.authorizationUrl")
						.value(org.hamcrest.Matchers.containsString("state=")))
				.andExpect(jsonPath("$.state").isNotEmpty());
	}

	@Test
	void loginWithCodeReturnsJwt() throws Exception {
		String redirect = "http://localhost:4201/cb";
		String state = oauthBrowserBindingPort.issue(OAuthBrowserBindingPort.PROVIDER_GOOGLE, redirect);
		when(googleOAuthClient.exchangeCode(eq("auth-code"), eq(redirect)))
				.thenReturn(new GoogleUserProfile("gid-1", "user@example.com", true, "User"));
		when(authUserAccountPort.findByProviderSubject("google", "gid-1"))
				.thenReturn(java.util.Optional.of(
						new AuthUserAccount(7L, "user@example.com", "User", true)));

		mockMvc.perform(post("/api/int/auth/oauth/google")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"authorizationCode":"auth-code","redirectUri":"%s","state":"%s"}
								""".formatted(redirect, state)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.expiresAtEpochSeconds").isNumber());
	}

	@Test
	void loginWithoutValidStateIsRejected() throws Exception {
		mockMvc.perform(post("/api/int/auth/oauth/google")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"authorizationCode":"bad","redirectUri":"http://x","state":"nope"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authorizeThenLoginRoundTrip() throws Exception {
		googleOAuthProperties.setClientId("client-abc");
		googleOAuthProperties.setClientSecret("secret-xyz");
		String redirect = "http://localhost:4201/auth/callback/google";
		MvcResult auth = mockMvc.perform(get("/api/int/auth/oauth/google/authorize-url")
						.param("redirectUri", redirect))
				.andExpect(status().isOk())
				.andReturn();
		JsonNode body = objectMapper.readTree(auth.getResponse().getContentAsString());
		String state = body.get("state").asText();

		when(googleOAuthClient.exchangeCode(eq("auth-code"), eq(redirect)))
				.thenReturn(new GoogleUserProfile("gid-2", "u@e.com", true, "U"));
		when(authUserAccountPort.findByProviderSubject("google", "gid-2"))
				.thenReturn(java.util.Optional.of(new AuthUserAccount(8L, "u@e.com", "U", true)));

		mockMvc.perform(post("/api/int/auth/oauth/google")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"authorizationCode":"auth-code","redirectUri":"%s","state":"%s"}
								""".formatted(redirect, state)))
				.andExpect(status().isOk());
	}
}
