package com.appv4.auth.infrastructure.adapter.in.web;

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

import com.appv4.auth.application.port.out.AuthUserAccountPort;
import com.appv4.auth.application.port.out.AuthUserAccountPort.AuthUserAccount;
import com.appv4.auth.application.port.out.FacebookOAuthClient;
import com.appv4.auth.application.port.out.FacebookOAuthClient.FacebookUserProfile;
import com.appv4.auth.infrastructure.config.FacebookOAuthProperties;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FacebookOAuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private FacebookOAuthProperties facebookOAuthProperties;

	@MockBean
	private FacebookOAuthClient facebookOAuthClient;

	@MockBean
	private AuthUserAccountPort authUserAccountPort;

	@BeforeEach
	void resetFacebookProps() {
		facebookOAuthProperties.setAppId("");
		facebookOAuthProperties.setAppSecret("");
	}

	@Test
	void authorizeUrlRequiresConfig() throws Exception {
		mockMvc.perform(get("/api/int/auth/oauth/facebook/authorize-url")
						.param("redirectUri", "http://localhost:4201/auth/callback/facebook"))
				.andExpect(status().isServiceUnavailable());
	}

	@Test
	void authorizeUrlReturnsFacebookUrlWhenConfigured() throws Exception {
		facebookOAuthProperties.setAppId("app-123");
		facebookOAuthProperties.setAppSecret("secret-xyz");

		mockMvc.perform(get("/api/int/auth/oauth/facebook/authorize-url")
						.param("redirectUri", "http://localhost:4201/auth/callback/facebook"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.authorizationUrl")
						.value(org.hamcrest.Matchers.containsString("app-123")))
				.andExpect(jsonPath("$.authorizationUrl")
						.value(org.hamcrest.Matchers.containsString("facebook.com")));
	}

	@Test
	void loginWithCodeReturnsJwt() throws Exception {
		when(facebookOAuthClient.exchangeCode(eq("auth-code"), eq("http://localhost:4201/cb")))
				.thenReturn(new FacebookUserProfile("fb-1", "user@example.com", "User"));
		when(authUserAccountPort.findByProviderSubject("facebook", "fb-1"))
				.thenReturn(Optional.of(new AuthUserAccount(7L, "user@example.com", "User", true)));

		mockMvc.perform(post("/api/int/auth/oauth/facebook")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"authorizationCode":"auth-code","redirectUri":"http://localhost:4201/cb"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.expiresAtEpochSeconds").isNumber());
	}

	@Test
	void loginWithoutEmailStillPublic() throws Exception {
		when(facebookOAuthClient.exchangeCode(eq("bad"), eq("http://x")))
				.thenReturn(new FacebookUserProfile("fb", null, "A"));

		mockMvc.perform(post("/api/int/auth/oauth/facebook")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"authorizationCode":"bad","redirectUri":"http://x"}
								"""))
				.andExpect(status().isForbidden());
	}
}
