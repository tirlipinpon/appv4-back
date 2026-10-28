package com.appv4.auth.application.service.question;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort.DraftContent;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/** Validation propre à une forme. Le contenu validé peut avoir n'importe quelle structure typée. */
public interface QuestionFormHandler {

	FormLine form();

	Prepared prepare(String prompt, ContentInput input);

	record Prepared(String prompt, DraftContent content) {
	}
}
