package com.appv4.auth.application.result;

import java.util.List;

/**
 * Matières du professeur, regroupées comme son profil (SCRUM-161).
 *
 * <p><b>Business :</b> les écoles suivent l'ordre du profil. Dans une école, les
 * années suivent la liste fermée. Les matières d'une année sont triées par nom.
 * Une année sans matière a une liste vide : l'écran affiche « Aucune matière ».
 * <p><b>Technique :</b> pas d'exercice, pas de forme de question, pas d'élève.
 * <p><b>Métier / flow :</b> résultat de {@code GET /api/int/teacher/subjects}.
 */
public record TeacherSubjectsView(List<SchoolGroup> schools) {

	public TeacherSubjectsView {
		schools = schools == null ? List.of() : List.copyOf(schools);
	}

	/**
	 * @param schoolName nom du référentiel, vide si l'école n'y est plus
	 */
	public record SchoolGroup(long schoolId, String schoolName, List<GradeGroup> grades) {

		public SchoolGroup {
			grades = grades == null ? List.of() : List.copyOf(grades);
		}
	}

	public record GradeGroup(String grade, List<SubjectLine> subjects) {

		public GradeGroup {
			subjects = subjects == null ? List.of() : List.copyOf(subjects);
		}
	}

	/**
	 * Le nom affiché, tel qu'il a été saisi (espaces autour déjà retirés).
	 */
	public record SubjectLine(long id, String name) {
	}
}
