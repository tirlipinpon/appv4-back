package com.appv4.auth.application.service.question;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;
import com.appv4.auth.application.service.TeacherQuestionService;

/** Catalogue des formes réellement implémentées. Refuse les codes en double au démarrage. */
public final class QuestionFormRegistry {

	private final Map<String, QuestionFormHandler> handlers;

	public QuestionFormRegistry(List<QuestionFormHandler> handlers) {
		this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
				handler -> handler.form().code(), Function.identity()));
	}

	public List<FormLine> forms() {
		return handlers.values().stream().map(QuestionFormHandler::form)
				.sorted(Comparator.comparingInt(FormLine::sortOrder).thenComparing(FormLine::code)).toList();
	}

	public QuestionFormHandler require(String formCode) {
		QuestionFormHandler handler = handlers.get(formCode == null ? "" : formCode.trim());
		if (handler == null) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, TeacherQuestionService.FORM_UNKNOWN);
		}
		return handler;
	}

	public String labelOf(String formCode) {
		QuestionFormHandler handler = handlers.get(formCode);
		return handler == null ? formCode : handler.form().label();
	}
}
