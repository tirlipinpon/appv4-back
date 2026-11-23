package com.appv4.auth.application.result;

import java.util.List;

/**
 * Formes de questions que le professeur peut préparer aujourd'hui (SCRUM-174).
 *
 * <p><b>Business :</b> la liste ne contient que les formes dont le formulaire
 * existe. Une forme annoncée sans écran n'y est pas.
 * <p><b>Technique :</b> code stable ({@code qcm}), libellé affiché, ordre.
 * <p><b>Métier / flow :</b> résultat de {@code GET /api/int/teacher/question-forms}.
 */
public record TeacherQuestionFormsView(List<FormLine> forms) {

	public record FormLine(String code, String label, int sortOrder) {
	}
}
