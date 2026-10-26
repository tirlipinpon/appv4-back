package com.appv4.auth.application.result;

import java.util.List;

/**
 * Liste des enfants et écoles proposables (SCRUM-156).
 *
 * <p><b>Business :</b> le parent retrouve ses fiches. Le PIN n'est pas dans cette vue.
 * <p><b>Technique :</b> objet de sortie du cas d'usage, pas le JSON HTTP.
 */
public record ParentChildrenView(
		List<ChildCard> children,
		List<SchoolChoice> schools,
		List<GradeList> curricula) {

	public ParentChildrenView {
		children = children == null ? List.of() : List.copyOf(children);
		schools = schools == null ? List.of() : List.copyOf(schools);
		curricula = curricula == null ? List.of() : List.copyOf(curricula);
	}

	/** @param country France ou Belgique, valeur envoyée à l'enregistrement */
	public record GradeList(String country, List<String> grades) {
		public GradeList {
			grades = grades == null ? List.of() : List.copyOf(grades);
		}
	}

	public record ChildCard(
			long id,
			String firstName,
			String lastName,
			String sex,
			int age,
			String country,
			String curriculum,
			String grade,
			Long schoolId,
			String schoolName,
			String avatarCode,
			boolean hasPhoto,
			boolean active,
			List<String> linkPhrases,
			int schoolSubjectCount,
			int extraSubjectCount,
			List<KinshipMark> links) {

		public ChildCard {
			linkPhrases = linkPhrases == null ? List.of() : List.copyOf(linkPhrases);
			links = links == null ? List.of() : List.copyOf(links);
		}
	}

	/**
	 * Lien court vers un autre enfant du même parent.
	 *
	 * <p><b>Business :</b> la liste montre sa photo ou son avatar, et le mot (frère, sœur…).
	 * <p><b>Technique :</b> {@code otherChildId} désigne l'autre fiche. {@code label} est le mot court.
	 * <p><b>Métier / flow :</b> relu avec la liste, après l'enregistrement du lien (SCRUM-158).
	 */
	public record KinshipMark(long otherChildId, String label) {
	}

	/**
	 * @param grades classes de cette école, déjà filtrées par son pays et son type
	 */
	public record SchoolChoice(long id, String name, String country, List<String> grades) {
		public SchoolChoice {
			grades = grades == null ? List.of() : List.copyOf(grades);
		}
	}
}
