package com.appv4.auth.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.appv4.auth.application.service.RecoverIdentifierService;
import com.appv4.auth.infrastructure.config.AuthRateLimitProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;

class RecoverIdentifierRateLimitTest {

	@Test
	void thirdCallAsksToWaitAndSendsNoMask() throws Exception {
		AuthRateLimitProperties properties = new AuthRateLimitProperties();
		properties.setEnabled(true);
		properties.setMaxRequests(2);
		properties.setWindowSeconds(60);
		AuthRateLimitFilter filter = new AuthRateLimitFilter(properties, new InMemoryAuthRateLimiter(2, 60));
		FilterChain chain = (request, response) -> ((HttpServletResponse) response).setStatus(200);

		filter.doFilter(request(), new MockHttpServletResponse(), chain);
		filter.doFilter(request(), new MockHttpServletResponse(), chain);
		MockHttpServletResponse blocked = new MockHttpServletResponse();
		filter.doFilter(request(), blocked, chain);

		assertThat(blocked.getStatus()).isEqualTo(429);
		assertThat(blocked.getContentAsString()).contains(RecoverIdentifierService.WAIT);
		assertThat(blocked.getContentAsString()).doesNotContain("maskedEmail");
	}

	private static MockHttpServletRequest request() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/int/auth/recover-identifier");
		request.setRemoteAddr("203.0.113.8");
		return request;
	}
}
