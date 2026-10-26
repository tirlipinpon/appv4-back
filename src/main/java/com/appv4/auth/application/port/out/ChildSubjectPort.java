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

	List<StoredLink> linksOf(long childId);

	List<CountedLink> visibleLinksForParent(long parentUserId);

	/** Crée le lien, ou met à jour {@code excluded} s'il existe déjà. */
	void save(long childId, long subjectId, boolean excluded);

	/**
	 * Efface le lien. Sert aux cours extra-scolaires.
	 *
	 * @return faux s'il n'y avait pas de lien
	 */
	boolean deleteLink(long childId, long subjectId);

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
}
