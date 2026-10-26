package com.appv4.auth.application.result;

import java.util.List;

/**
 * Élèves rangés comme le profil du professeur (SCRUM-160).
 *
 * <p><b>Business :</b> une école du profil, puis chaque année cochée.
 * Une année sans enfant reste visible. Le PIN et le parent ne sont pas là.
 * <p><b>Technique :</b> objet de sortie du cas d'usage, pas le JSON HTTP.
 * <p><b>Métier / flow :</b> le controller le traduit vers la réponse OpenAPI.
 */
public record TeacherStudentsView(String noClassMessage, List<SchoolGroup> schools) {

	public TeacherStudentsView {
		schools = schools == null ? List.of() : List.copyOf(schools);
	}

	/**
	 * @param curriculumLabel nom de la norme, France ou Belgique francophone
	 */
	public record SchoolGroup(
			long schoolId,
			String schoolName,
			String curriculumLabel,
			List<GradeGroup> grades) {

		public SchoolGroup {
			grades = grades == null ? List.of() : List.copyOf(grades);
		}
	}

	/**
	 * @param countLabel   « 0 élève », « 1 élève » ou « 2 élèves »
	 * @param emptyMessage phrase affichée quand la classe n'a personne
	 */
	public record GradeGroup(
			String grade,
			String countLabel,
			String emptyMessage,
			List<StudentLine> students) {

		public GradeGroup {
			students = students == null ? List.of() : List.copyOf(students);
		}
	}

	public record StudentLine(
			long id,
			String firstName,
			String lastName,
			int age,
			String avatarCode,
			boolean hasPhoto) {
	}
}
