package com.appv4.auth.application.result;

import java.util.List;

/**
 * Profil professeur renvoyé à l'écran (SCRUM-159).
 *
 * <p><b>Business :</b> l'email se lit, le nom se corrige, les écoles se choisissent
 * dans la liste disponible. Une liste vide veut dire : rien à proposer.
 * <p><b>Technique :</b> objet de sortie du cas d'usage, pas le JSON HTTP.
 * <p><b>Métier / flow :</b> le controller le traduit vers la réponse OpenAPI.
 */
public record TeacherProfileView(
		String email,
		String displayName,
		List<AssignedSchool> schools,
		List<AvailableSchool> availableSchools,
		List<CurriculumChoice> curricula) {

	public TeacherProfileView {
		schools = schools == null ? List.of() : List.copyOf(schools);
		availableSchools = availableSchools == null ? List.of() : List.copyOf(availableSchools);
		curricula = curricula == null ? List.of() : List.copyOf(curricula);
	}

	/**
	 * @param schoolName nom lu dans le référentiel, vide si l'école n'y est plus
	 */
	public record AssignedSchool(long schoolId, String schoolName, String curriculum, List<String> grades) {
		public AssignedSchool {
			grades = grades == null ? List.of() : List.copyOf(grades);
		}
	}

	public record AvailableSchool(long id, String name) {
	}

	/**
	 * @param code   {@code FRANCE} ou {@code BELGIUM_FR}
	 * @param grades années proposées pour cette norme, dans l'ordre d'affichage
	 */
	public record CurriculumChoice(String code, String label, List<String> grades) {
		public CurriculumChoice {
			grades = grades == null ? List.of() : List.copyOf(grades);
		}
	}
}
