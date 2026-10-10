package com.appv4.shared.domain;

import java.util.Objects;

/**
 * Exception métier. Le mapping HTTP (ProblemDetail) se fera dans l'adaptateur REST.
 */
public class DomainException extends RuntimeException {

	private final ErrorCode errorCode;

	public DomainException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
