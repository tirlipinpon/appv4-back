package com.appv4.auth.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.appv4.auth.application.exception.AuthDomainException;

/**
 * Mappe les erreurs métier auth → HTTP + corps JSON simple.
 */
@RestControllerAdvice(basePackages = "com.appv4.auth.infrastructure.adapter.in.web")
public class AuthExceptionHandler {

	@ExceptionHandler(AuthDomainException.class)
	public ResponseEntity<ErrorBody> handleAuthDomain(AuthDomainException ex) {
		HttpStatus status = HttpStatus.resolve(ex.getHttpStatus());
		if (status == null) {
			status = HttpStatus.BAD_REQUEST;
		}
		return ResponseEntity.status(status).body(new ErrorBody(ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) {
		return ResponseEntity.badRequest().body(new ErrorBody("Requête invalide"));
	}

	public record ErrorBody(String message) {
	}
}
