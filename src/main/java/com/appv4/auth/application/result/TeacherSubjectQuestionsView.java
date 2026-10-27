package com.appv4.auth.application.result;

import java.util.List;

/**
 * Questions déjà préparées dans une matière (SCRUM-174).
 *
 * <p><b>Business :</b> le professeur revoit l'énoncé, les propositions d'un
 * choix multiple, ou les paires d'un Memory. L'ordre enregistré est celui saisi.
 * <p><b>Technique :</b> {@code formCode} est le code choisi dans la liste des formes.
 * <p><b>Métier / flow :</b> résultat de {@code GET/POST /api/int/teacher/subjects/{id}/questions}.
 */
public record TeacherSubjectQuestionsView(List<QuestionLine> questions) {

	public record QuestionLine(
			long id,
			String formCode,
			String formLabel,
			String prompt,
			List<ChoiceLine> choices,
			List<PairLine> pairs) {
	}

	public record ChoiceLine(long id, String label, boolean correct) {
	}

	public record PairLine(long id, String questionSide, String answerSide) {
	}
}
