package com.appv4.auth.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.appv4.auth.application.exception.AuthErrorCode;

class AuthExceptionHandlerTest {

	@Test
	void mapsCodesToHttpWithoutLeakingStatusIntoDomain() {
		assertThat(AuthExceptionHandler.toHttp(AuthErrorCode.UNAUTHORIZED)).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(AuthExceptionHandler.toHttp(AuthErrorCode.FORBIDDEN)).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(AuthExceptionHandler.toHttp(AuthErrorCode.NOT_FOUND)).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(AuthExceptionHandler.toHttp(AuthErrorCode.CONFLICT)).isEqualTo(HttpStatus.CONFLICT);
		assertThat(AuthExceptionHandler.toHttp(AuthErrorCode.SERVICE_UNAVAILABLE))
				.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
		assertThat(AuthExceptionHandler.toHttp(AuthErrorCode.INTERNAL_ERROR))
				.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(AuthExceptionHandler.toHttp(AuthErrorCode.BAD_REQUEST)).isEqualTo(HttpStatus.BAD_REQUEST);
	}
}
