package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.PROMPT_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.PROMPT_TOO_LONG;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;

final class QuestionValidation {

	private static final int PROMPT_MAX = 500;

	private QuestionValidation() {
	}

	static String requirePrompt(String prompt) {
		String statement = prompt == null ? "" : prompt.trim();
		if (statement.isEmpty()) {
			throw bad(PROMPT_REQUIRED);
		}
		if (statement.length() > PROMPT_MAX) {
			throw bad(PROMPT_TOO_LONG);
		}
		return statement;
	}

	static String optionalPrompt(String prompt) {
		String statement = prompt == null ? "" : prompt.trim();
		if (statement.length() > PROMPT_MAX) {
			throw bad(PROMPT_TOO_LONG);
		}
		return statement;
	}

	static AuthDomainException bad(String message) {
		return new AuthDomainException(AuthErrorCode.BAD_REQUEST, message);
	}
}
