package com.appv4.auth.application.service.question.play;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.ChildQuestionPlayUseCase.StatementAnswer;
import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredQuestion;
import com.appv4.auth.application.port.out.TeacherQuestionPort.StoredStatement;

/**
 * Note un vrai/faux enfant : une réponse par phrase, score = % de phrases justes.
 *
 * <p><b>Business :</b> réussi seulement si toutes les phrases sont correctement marquées.
 * <p><b>Technique :</b> compare value à {@code truthful}.
 */
public final class TrueFalseChildAnswerGrader {

	public static final String FORM = "vrai_faux";
	public static final String BAD_ANSWER = "Réponse invalide pour cette question.";

	private TrueFalseChildAnswerGrader() {
	}

	public static ChildAnswerGrade grade(StoredQuestion question, List<StatementAnswer> answers) {
		List<StoredStatement> statements = question.statements();
		if (statements.isEmpty()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Set<Long> known = statements.stream().map(StoredStatement::id).collect(Collectors.toSet());
		List<StatementAnswer> rows = answers == null ? List.of() : answers;
		if (rows.size() != statements.size()) {
			throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
		}
		Set<Long> seen = new HashSet<>();
		Map<Long, Boolean> byId = new HashMap<>();
		for (StatementAnswer row : rows) {
			if (!known.contains(row.statementId()) || !seen.add(row.statementId())) {
				throw new AuthDomainException(AuthErrorCode.BAD_REQUEST, BAD_ANSWER);
			}
			byId.put(row.statementId(), row.value());
		}
		int ok = 0;
		List<AnswerLine> lines = new ArrayList<>();
		for (StoredStatement statement : statements) {
			boolean value = byId.get(statement.id());
			boolean correct = value == statement.truthful();
			if (correct) {
				ok++;
			}
			lines.add(new AnswerLine("statement", statement.id(), value, null, correct));
		}
		int score = (int) Math.round(100.0 * ok / statements.size());
		boolean passed = ok == statements.size();
		return new ChildAnswerGrade(score, passed, lines);
	}
}
