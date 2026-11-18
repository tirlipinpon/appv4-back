package com.appv4.auth.application.port.out;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.appv4.auth.application.port.out.ChildPort.ChildSaved;

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

	/**
	 * Tous les liens matière des enfants encore affichés de ce parent.
	 *
	 * <p><b>Business :</b> la page matières charge la famille en une fois.
	 * <p><b>Technique :</b> défaut : une lecture par enfant. L'adapter Postgres regroupe.
	 */
	default List<ChildLink> linksForParent(long parentUserId, List<ChildSaved> children) {
		if (children == null || children.isEmpty()) {
			return List.of();
		}
		List<ChildLink> rows = new java.util.ArrayList<>();
		for (ChildSaved child : children) {
			for (StoredLink link : linksOf(child.id())) {
				rows.add(new ChildLink(child.id(), link.subjectId(), link.excluded()));
			}
		}
		return rows;
	}

	List<CountedLink> visibleLinksForParent(long parentUserId);

	/** Crée le lien, ou met à jour {@code excluded} s'il existe déjà. */
	void save(long childId, long subjectId, boolean excluded);

	/** Insert a default link only when absent, preserving any exclusion. */
	void ensureDefault(long childId, long subjectId);

	/**
	 * Rattache en une fois les matières visibles de la classe.
	 *
	 * <p><b>Business :</b> un cours de l'école et de l'année apparaît dans la liste
	 * sans que le parent le cherche. Un cours déjà retiré reste retiré.
	 * <p><b>Technique :</b> un seul insert. La ligne déjà là n'est pas réécrite.
	 * <p><b>Métier / flow :</b> court, et terminé avant la lecture de la page.
	 */
	default void ensureMissingClassLinks(List<ChildSaved> children) {
		if (children == null) {
			return;
		}
		for (ChildSaved child : children) {
			if (child.schoolId() == null || child.grade() == null || child.grade().isBlank()) {
				continue;
			}
			Set<Long> known = new HashSet<>();
			for (StoredLink link : linksOf(child.id())) {
				known.add(link.subjectId());
			}
			for (CatalogSubject subject : visibleInClass(child.schoolId(), child.grade())) {
				if (!known.contains(subject.id())) {
					ensureDefault(child.id(), subject.id());
				}
			}
		}
	}

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
	 * Lien matière d'un enfant, pour une lecture groupée par parent.
	 */
	record ChildLink(long childId, long subjectId, boolean excluded) {
	}

	/**
	 * @param grade année de la matière, le libellé stocké (CE1, 3e primaire)
	 */
	record CatalogSubject(
			long id,
			String name,
			String teacherName,
			long schoolId,
			String grade,
			String iconCode) {
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
