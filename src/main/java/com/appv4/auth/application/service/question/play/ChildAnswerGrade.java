package com.appv4.auth.application.service.question.play;

import java.util.List;

import com.appv4.auth.application.port.out.ChildQuestionAttemptPort.AnswerLine;

/**
 * Résultat interne du correcteur (score + lignes à stocker).
 */
public record ChildAnswerGrade(int scorePercent, boolean passed, List<AnswerLine> answers) {
	public ChildAnswerGrade {
		answers = answers == null ? List.of() : List.copyOf(answers);
	}
}
