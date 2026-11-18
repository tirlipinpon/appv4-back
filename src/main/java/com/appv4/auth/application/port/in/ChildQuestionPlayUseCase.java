package com.appv4.auth.application.port.in;

import java.util.List;

import com.appv4.auth.application.result.ChildAttemptResultView;
import com.appv4.auth.application.result.ChildPlayQuestionView;

/**
 * Lire une question jouable et enregistrer une tentative (BACK-16).
 *
 * <p><b>Business :</b> jeton enfant seulement ; matière liée obligatoire.
 * <p><b>Technique :</b> GET détail sans solution + POST answers → score.
 * <p><b>Métier / flow :</b> Learn play → ce port → ports out questions / attempts.
 */
public interface ChildQuestionPlayUseCase {

	ChildPlayQuestionView question(String rawToken, long subjectId, long questionId);

	ChildAttemptResultView submit(
			String rawToken, long subjectId, long questionId, AttemptAnswers answers);

	record StatementAnswer(long statementId, boolean value) {
	}

	record AttemptAnswers(List<Long> selectedChoiceIds, List<StatementAnswer> statements) {
		public AttemptAnswers {
			selectedChoiceIds = selectedChoiceIds == null ? List.of() : List.copyOf(selectedChoiceIds);
			statements = statements == null ? List.of() : List.copyOf(statements);
		}
	}
}
