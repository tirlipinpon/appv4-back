package com.appv4.auth.application.result;

import java.util.List;

/**
 * Ce qu'un professeur doit voir avant de retirer une année ou une école (SCRUM-201).
 *
 * <p><b>Business :</b> les élèves de la classe, le nombre de matières, et qui suit
 * chacune. Rien n'est retiré par cette lecture.
 * <p><b>Technique :</b> {@code grade} est vide quand l'aperçu porte sur l'école entière.
 * Les phrases et les comptes sont ceux à afficher.
 * <p><b>Métier / flow :</b> résultat de {@code GET /api/int/teacher/profile/removal-preview}.
 */
public record TeacherRemovalPreviewView(
		long schoolId,
		String schoolName,
		String grade,
		int studentCount,
		String studentCountLabel,
		String emptyStudentsLabel,
		List<StudentLine> students,
		int subjectCount,
		String subjectCountLabel,
		String emptySubjectsLabel,
		List<SubjectLine> subjects) {

	public TeacherRemovalPreviewView {
		students = List.copyOf(students);
		subjects = List.copyOf(subjects);
	}

	/**
	 * @param grade année de l'élève, le libellé de sa fiche
	 */
	public record StudentLine(long id, String firstName, String lastName, String grade) {
	}

	/**
	 * @param followerNames « prénom nom », déjà triés
	 */
	public record SubjectLine(
			long id,
			String name,
			String grade,
			int followerCount,
			String followerWarning,
			List<String> followerNames) {

		public SubjectLine {
			followerNames = List.copyOf(followerNames);
		}
	}
}
