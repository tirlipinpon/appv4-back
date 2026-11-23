package com.appv4.auth.application.exception;

/**
 * Erreur métier auth — sans statut HTTP (issue #30).
 *
 * <p><b>Business :</b> le cœur dit « interdit / introuvable », pas « 403 ».
 * <p><b>Technique :</b> {@link AuthErrorCode} ; {@code AuthExceptionHandler} traduit en HTTP.
 * <p><b>Métier / flow :</b> services / ports → cette exception → adaptateur web.
 */
public class AuthDomainException extends RuntimeException {

	private final AuthErrorCode errorCode;

	public AuthDomainException(AuthErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode == null ? AuthErrorCode.BAD_REQUEST : errorCode;
	}

	public AuthErrorCode getErrorCode() {
		return errorCode;
	}
}
