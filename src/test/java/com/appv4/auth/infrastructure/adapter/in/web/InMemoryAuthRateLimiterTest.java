package com.appv4.auth.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InMemoryAuthRateLimiterTest {

	@Test
	void acceptsUntilMaxThenRejectsWithinWindow() {
		InMemoryAuthRateLimiter limiter = new InMemoryAuthRateLimiter(3, 60);
		long t0 = 1_000_000L;

		assertThat(limiter.tryAcquire("ip|/login", t0)).isTrue();
		assertThat(limiter.tryAcquire("ip|/login", t0 + 1)).isTrue();
		assertThat(limiter.tryAcquire("ip|/login", t0 + 2)).isTrue();
		assertThat(limiter.tryAcquire("ip|/login", t0 + 3)).isFalse();
	}

	@Test
	void otherKeyIsIndependent() {
		InMemoryAuthRateLimiter limiter = new InMemoryAuthRateLimiter(1, 60);
		long t0 = 1_000_000L;

		assertThat(limiter.tryAcquire("a|/login", t0)).isTrue();
		assertThat(limiter.tryAcquire("b|/login", t0)).isTrue();
		assertThat(limiter.tryAcquire("a|/login", t0 + 1)).isFalse();
	}

	@Test
	void windowExpiryFreesSlot() {
		InMemoryAuthRateLimiter limiter = new InMemoryAuthRateLimiter(1, 1);
		long t0 = 1_000_000L;

		assertThat(limiter.tryAcquire("ip|/login", t0)).isTrue();
		assertThat(limiter.tryAcquire("ip|/login", t0 + 500)).isFalse();
		assertThat(limiter.tryAcquire("ip|/login", t0 + 1001)).isTrue();
	}
}
