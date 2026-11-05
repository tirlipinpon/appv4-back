package com.appv4.auth.application.port.out;

import java.util.List;

/**
 * Matières qu'un parent rattache à un enfant (SCRUM-162).
 *
 * <p><b>Business :</b> le parent choisit une matière déjà créée. Il peut la retirer
 * de la liste de cet enfant. La matière du professeur reste en base.
 * <p><b>Technique :</b> {@code back_child_subject_links}. Seules les matières
 * actives et non cachées sont proposées ou comptées.
 * <p><b>Métier / flow :</b> port out de {@code /api/int/parent/children/{id}/subjects}.
 */
public interface ChildSubjectPort {

	List<CatalogSubject> visibleSubjects();

	/** Visible subjects in one class, used for default enrollment. */
	default List<CatalogSubject> visibleInClass(long schoolId, String grade) {
		return visibleSubjects().stream()
				.filter(subject -> subject.schoolId() == schoolId && grade.equals(subject.grade())).toList();
	}

	/** Visible subjects corresponding to the requested identifiers. */
	default List<CatalogSubject> visibleByIds(List<Long> ids) {
		return visibleSubjects().stream().filter(subject -> ids.contains(subject.id())).toList();
	}

	/** Bounded search of offers not currently selected for this child. */
	default List<CatalogSubject> searchOffers(long childId, String query, long schoolId, String grade,
			long childSchoolId, String childGrade, long afterId, int limit) {
		List<StoredLink> links = linksOf(childId);
		List<Long> chosen = links.stream().filter(link -> !link.excluded()).map(StoredLink::subjectId).toList();
		List<Long> excluded = links.stream().filter(StoredLink::excluded).map(StoredLink::subjectId).toList();
		return visibleSubjects().stream().filter(subject -> subject.id() > afterId && !chosen.contains(subject.id()))
				.filter(subject -> subject.schoolId() != childSchoolId || !childGrade.equals(subject.grade())
						|| excluded.contains(subject.id()))
				.filter(subject -> schoolId == 0 || subject.schoolId() == schoolId)
				.filter(subject -> grade.isEmpty() || grade.equals(subject.grade()))
				.filter(subject -> subject.name().toLowerCase(java.util.Locale.ROOT).contains(query))
				.sorted(java.util.Comparator.comparingLong(CatalogSubject::id)).limit(limit).toList();
	}

	List<StoredLink> linksOf(long childId);

	default java.util.Optional<StoredLink> findLink(long childId, long subjectId) {
		return linksOf(childId).stream().filter(link -> link.subjectId() == subjectId).findFirst();
	}

	List<CountedLink> visibleLinksForParent(long parentUserId);

	/** Crée le lien, ou met à jour {@code excluded} s'il existe déjà. */
	void save(long childId, long subjectId, boolean excluded);

	/** Insert a default link only when absent, preserving any exclusion. */
	void ensureDefault(long childId, long subjectId);

	/**
	 * Efface le lien. Sert aux cours extra-scolaires.
	 *
	 * @return faux s'il n'y avait pas de lien
	 */
	boolean deleteLink(long childId, long subjectId);

	/**
	 * Enfants encore proposés dans cette école et cette année.
	 * Une fiche masquée ou désactivée n'est pas renvoyée : le professeur ne la voit plus.
	 * Une année vide ne renvoie personne.
	 */
	List<FollowingChild> activeInClass(long schoolId, String grade);

	/**
	 * Liens de cette matière vers des enfants encore proposés.
	 * {@code excluded} vrai : le parent a retiré le cours de la classe.
	 */
	List<SubjectChildLink> linksOfSubject(long subjectId);

	record StoredLink(long subjectId, boolean excluded) {
	}

	/**
	 * @param grade année de la matière, le libellé stocké (CE1, 3e primaire)
	 */
	record CatalogSubject(long id, String name, String teacherName, long schoolId, String grade) {
	}

	/**
	 * Lien encore visible : la matière est active et n'est pas cachée.
	 */
	record CountedLink(long childId, long schoolId, String grade) {
	}

	/**
	 * Fiche encore proposée. L'identifiant sert à ne compter l'enfant qu'une fois.
	 */
	record FollowingChild(long id, String firstName, String lastName) {
	}

	/**
	 * Lien entre une matière et un enfant encore proposé.
	 *
	 * @param excluded vrai si le parent a retiré ce cours
	 */
	record SubjectChildLink(FollowingChild child, boolean excluded) {
	}
}
