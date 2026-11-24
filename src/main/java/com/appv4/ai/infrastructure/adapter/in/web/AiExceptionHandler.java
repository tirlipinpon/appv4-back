package com.appv4.ai.infrastructure.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.appv4.ai.application.BesoinIaException;

/**
 * Erreurs IA vers HTTP (BACK-84).
 *
 * <p><b>Business :</b> le message renvoyé est celui du métier, sans clé.
 * <p><b>Technique :</b> 400, 403 ou 404 selon le code.
 * <p><b>Métier / flow :</b> le controller laisse passer l'exception, cet avis la traduit.
 */
@RestControllerAdvice(basePackages = "com.appv4.ai.infrastructure.adapter.in.web")
public class AiExceptionHandler {

	@ExceptionHandler(BesoinIaException.class)
	public ResponseEntity<ErrorBody> handle(BesoinIaException ex) {
		HttpStatus status = switch (ex.code()) {
			case FORBIDDEN -> HttpStatus.FORBIDDEN;
			case NOT_FOUND -> HttpStatus.NOT_FOUND;
			case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
		};
		return ResponseEntity.status(status).body(new ErrorBody(ex.getMessage()));
	}

	public record ErrorBody(String message) {
	}
}
