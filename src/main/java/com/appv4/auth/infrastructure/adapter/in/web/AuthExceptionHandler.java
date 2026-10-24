package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Mappe les erreurs métier auth → HTTP + corps JSON simple (issue #30).
 */
@RestControllerAdvice(basePackages = "com.appv4.auth.infrastructure.adapter.in.web")
public class AuthExceptionHandler {

	@ExceptionHandler(AuthDomainException.class)
	public ResponseEntity<ErrorBody> handleAuthDomain(AuthDomainException ex) {
		return ResponseEntity.status(toHttp(ex.getErrorCode())).body(new ErrorBody(ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) {
		return ResponseEntity.badRequest().body(new ErrorBody("Requête invalide"));
	}

	static HttpStatus toHttp(AuthErrorCode code) {
		return switch (code) {
			case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
			case FORBIDDEN -> HttpStatus.FORBIDDEN;
			case NOT_FOUND -> HttpStatus.NOT_FOUND;
			case CONFLICT -> HttpStatus.CONFLICT;
			case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
			case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
			case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
		};
	}

	@Schema(name = "AuthErrorBody")
	public record ErrorBody(String message) {
	}
}
