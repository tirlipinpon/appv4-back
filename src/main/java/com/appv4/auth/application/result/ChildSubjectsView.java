package com.appv4.auth.application.result;

import java.util.List;

/**
 * Matières d'un enfant, vues par son parent (SCRUM-162).
 *
 * <p><b>Business :</b> déjà choisies, et encore proposables. L'étiquette scolaire
 * est calculée : même école et même année que l'enfant.
 * <p><b>Technique :</b> pas le JSON HTTP. Une matière désactivée ou cachée n'est pas ici.
 * <p><b>Métier / flow :</b> résultat de la page matières d'un enfant.
 */
public record ChildSubjectsView(
		long childId,
		String firstName,
		String lastName,
		Long schoolId,
		String schoolName,
		String grade,
		List<SubjectOffer> chosen,
		List<SubjectOffer> proposed) {

	public ChildSubjectsView {
		chosen = chosen == null ? List.of() : List.copyOf(chosen);
		proposed = proposed == null ? List.of() : List.copyOf(proposed);
	}

	/**
	 * @param scolaire vrai seulement si l'école et l'année sont celles de l'enfant maintenant
	 */
	public record SubjectOffer(
			long subjectId,
			String name,
			String teacherName,
			long schoolId,
			String schoolName,
			String grade,
			boolean scolaire) {
	}
}
