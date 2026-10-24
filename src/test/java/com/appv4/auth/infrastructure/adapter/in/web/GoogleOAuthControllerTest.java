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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.AuthUserAccountPort.SessionState;
import com.appv4.auth.application.port.out.GoogleOAuthClient;
import com.appv4.auth.application.port.out.GoogleOAuthClient.GoogleUserProfile;
import com.appv4.auth.infrastructure.config.GoogleOAuthProperties;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleOAuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private GoogleOAuthProperties googleOAuthProperties;

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
	void authorizeUrlReturnsGoogleUrlWhenConfigured() throws Exception {
		googleOAuthProperties.setClientId("client-abc");
		googleOAuthProperties.setClientSecret("secret-xyz");

		mockMvc.perform(get("/api/int/auth/oauth/google/authorize-url")
						.param("redirectUri", "http://localhost:4201/auth/callback/google"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authorizationUrl")
						.value(org.hamcrest.Matchers.containsString("client-abc")))
				.andExpect(jsonPath("$.authorizationUrl")
						.value(org.hamcrest.Matchers.containsString("accounts.google.com")));
	}

	@Test
	void loginWithCodeReturnsJwt() throws Exception {
		when(googleOAuthClient.exchangeCode(eq("auth-code"), eq("http://localhost:4201/cb")))
				.thenReturn(new GoogleUserProfile("gid-1", "user@example.com", true, "User"));
		when(authUserAccountPort.findByProviderSubject("google", "gid-1"))
				.thenReturn(java.util.Optional.of(
						new AuthUserAccount(7L, "user@example.com", "User", true)));

		mockMvc.perform(post("/api/int/auth/oauth/google")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"authorizationCode":"auth-code","redirectUri":"http://localhost:4201/cb"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.expiresAtEpochSeconds").isNumber());
	}

	@Test
	void loginWithoutTokenStillPublic() throws Exception {
		when(googleOAuthClient.exchangeCode(eq("bad"), eq("http://x")))
				.thenReturn(new GoogleUserProfile("g", "a@b.com", false, "A"));

		mockMvc.perform(post("/api/int/auth/oauth/google")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"authorizationCode":"bad","redirectUri":"http://x"}
								"""))
				.andExpect(status().isForbidden());
	}
}
