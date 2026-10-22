package com.appv4.auth.application.exception;

/**
 * Erreur métier auth (mappée en HTTP par le controller advice).
 */
public class AuthDomainException extends RuntimeException {

	private final int httpStatus;

	public AuthDomainException(int httpStatus, String message) {
		super(message);
		this.httpStatus = httpStatus;
	}

	public int getHttpStatus() {
		return httpStatus;
	}
}
