package com.appv4.auth.application.result;

import java.util.List;

/**
 * Questions déjà préparées dans une matière (SCRUM-174).
 *
 * <p><b>Business :</b> le professeur revoit l'énoncé, les propositions et
 * lesquelles sont justes. L'ordre enregistré est celui qu'il a saisi.
 * <p><b>Technique :</b> {@code formCode} est le code choisi dans la liste des formes.
 * <p><b>Métier / flow :</b> résultat de {@code GET/POST /api/int/teacher/subjects/{id}/questions}.
 */
public record TeacherSubjectQuestionsView(List<QuestionLine> questions) {

	public record QuestionLine(
			long id,
			String formCode,
			String formLabel,
			String prompt,
			List<ChoiceLine> choices) {
	}

	public record ChoiceLine(long id, String label, boolean correct) {
	}
}
