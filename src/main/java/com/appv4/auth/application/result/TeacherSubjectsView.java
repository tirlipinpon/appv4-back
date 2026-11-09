package com.appv4.auth.application.result;

import java.util.List;

/**
 * Matières du professeur, regroupées comme son profil (SCRUM-161, SCRUM-172).
 *
 * <p><b>Business :</b> les écoles suivent l'ordre du profil. Dans une école, les
 * années suivent la liste fermée. Les matières d'une année sont triées par nom.
 * Une année sans matière a une liste vide : l'écran affiche « Aucune matière ».
 * Les matières déjà créées par un collègue de la même classe sont aussi listées :
 * {@code followedByMe} est alors faux, et {@code sharedWith} nomme ceux qui la suivent.
 * Quand ce professeur la suit, {@code sharedWith} liste les co-profs.
 * {@code shareAskedTo} nomme les destinataires d'une demande de partage encore
 * en attente envoyée par ce professeur. Vide s'il n'y en a pas.
 * {@code ownedByMe} est vrai si ce professeur est le créateur / propriétaire.
 * {@code owner} nomme ce propriétaire, celui qui peut renommer.
 * Chaque année propose aussi les matières déjà créées dans une autre année du
 * professeur, tant que ce nom n'est pas déjà dans l'année affichée. Le libellé
 * donne le code postal, les années de création et le nom de l'école.
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

	/**
	 * @param addableCourses matières déjà créées ailleurs, absentes de cette année
	 */
	public record GradeGroup(String grade, List<SubjectLine> subjects, List<AddableCourse> addableCourses) {

		public GradeGroup {
			subjects = subjects == null ? List.of() : List.copyOf(subjects);
			addableCourses = addableCourses == null ? List.of() : List.copyOf(addableCourses);
		}
	}

	/**
	 * Matière déjà créée, que le professeur peut reprendre dans une autre année.
	 *
	 * <p><b>Business :</b> il reconnaît la matière par son nom, le code postal,
	 * les années où elle existe déjà et le nom de l'école.
	 * <p><b>Technique :</b> un nom identique (casse ignorée) dans la même école
	 * est une seule ligne, avec toutes ses années.
	 * <p><b>Métier / flow :</b> champ {@code addableCourses} de chaque année
	 * dans {@code GET /api/int/teacher/subjects}. Le front affiche {@code label}.
	 *
	 * @param name       nom affiché, celui de la première année trouvée
	 * @param schoolId   école où la matière a été créée
	 * @param schoolName nom du référentiel, vide si l'école n'y est plus
	 * @param postalCode code postal de cette école, vide s'il manque
	 * @param grades     années où ce nom existe déjà, dans l'ordre du profil
	 * @param label      texte prêt à afficher : nom, code postal, années, école
	 */
	public record AddableCourse(
			String name,
			long schoolId,
			String schoolName,
			String postalCode,
			List<String> grades,
			String label) {

		public AddableCourse {
			grades = grades == null ? List.of() : List.copyOf(grades);
			postalCode = postalCode == null ? "" : postalCode;
			schoolName = schoolName == null ? "" : schoolName;
			label = label == null ? "" : label;
		}
	}

	/**
	 * @param name          nom affiché, espaces autour déjà retirés
	 * @param active        accès du professeur connecté s'il la suit ; flag global sinon
	 * @param ownedByMe     vrai si {@code user_id} de la matière = connecté
	 * @param followedByMe  faux : matière déjà dans la classe, ce professeur ne la suit pas
	 * @param sharedWith    s'il la suit : les co-profs ; sinon : tous ceux qui la suivent
	 * @param shareAskedTo  destinataires d'un {@code ask} pending envoyé par le connecté
	 * @param owner         propriétaire : le professeur qui peut renommer
	 * @param questionCount total des questions enregistrées sur la matière (0 si aucune)
	 */
	public record SubjectLine(
			long id,
			String name,
			boolean active,
			boolean ownedByMe,
			boolean followedByMe,
			List<SharedHolder> sharedWith,
			List<SharedHolder> shareAskedTo,
			SharedHolder owner,
			long questionCount) {

		public SubjectLine {
			sharedWith = sharedWith == null ? List.of() : List.copyOf(sharedWith);
			shareAskedTo = shareAskedTo == null ? List.of() : List.copyOf(shareAskedTo);
			if (owner == null) {
				owner = new SharedHolder(0L, "");
			}
		}
	}

	public record SharedHolder(long userId, String displayName) {
	}
}
