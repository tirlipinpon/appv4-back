package com.appv4.auth.application.service.question;

import static com.appv4.auth.application.service.TeacherQuestionService.PHRASES_REQUIRED;
import static com.appv4.auth.application.service.TeacherQuestionService.PHRASE_TOO_LONG;
import static com.appv4.auth.application.service.TeacherQuestionService.SIDE_REQUIRED;
import static com.appv4.auth.application.service.question.QuestionValidation.bad;

import java.util.ArrayList;
import java.util.List;

import com.appv4.auth.application.port.in.TeacherQuestionUseCase.ContentInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.StatementInput;
import com.appv4.auth.application.port.in.TeacherQuestionUseCase.StatementsInput;
import com.appv4.auth.application.port.out.TeacherQuestionPort;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StatementsDraft;
import com.appv4.auth.application.result.TeacherQuestionFormsView.FormLine;

/** Règles de saisie de la forme vrai_faux. */
public final class TrueFalseQuestionForm implements QuestionFormHandler {

	private static final int PHRASE_MAX = 200;

	@Override
	public FormLine form() {
		return new FormLine("vrai_faux", "Vrai ou faux", 4);
	}

	@Override
	public Prepared prepare(String prompt, ContentInput input) {
		if (!(input instanceof StatementsInput statements)) {
			throw new IllegalArgumentException("Entrée incompatible avec vrai_faux");
		}
		return new Prepared(
				QuestionValidation.optionalPrompt(prompt),
				new StatementsDraft(requireStatements(statements.statements())));
	}

	private static List<TeacherQuestionPort.StatementDraft> requireStatements(List<StatementInput> statements) {
		List<TeacherQuestionPort.StatementDraft> kept = new ArrayList<>();
		if (statements != null) {
			for (StatementInput statement : statements) {
				if (statement == null) {
					continue;
				}
				String label = statement.label() == null ? "" : statement.label().trim();
				if (label.isEmpty()) {
					continue;
				}
				if (label.length() > PHRASE_MAX) {
					throw bad(PHRASE_TOO_LONG);
				}
				if (statement.truthful() == null) {
					throw bad(SIDE_REQUIRED);
				}
				kept.add(new TeacherQuestionPort.StatementDraft(label, statement.truthful()));
			}
		}
		if (kept.size() < 2) {
			throw bad(PHRASES_REQUIRED);
		}
		return List.copyOf(kept);
	}
}
