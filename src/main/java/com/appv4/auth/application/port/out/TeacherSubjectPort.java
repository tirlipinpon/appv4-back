package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Matières du professeur connecté (SCRUM-161).
 *
 * <p><b>Business :</b> une matière a un nom, une école et une année. Elle appartient
 * à un seul professeur. Un collègue de la même classe ne la voit pas.
 * <p><b>Technique :</b> {@code back_teacher_subjects}. {@code name_key} est le nom
 * en minuscules. {@code origin_subject_id} reste vide : un clone futur recopiera la
 * ligne vers un autre professeur. La table {@code back_subject_question_forms} peut
 * porter plusieurs formes pour une même matière (Mathématiques, par exemple). Cette
 * story n'écrit aucune forme et n'échange aucune matière.
 * <p><b>Métier / flow :</b> port out de {@code /api/int/teacher/subjects}. Quand le
 * profil retire une année, le service profil appelle {@link #hide} : la ligne reste,
 * elle n'est plus affichée.
 */
public interface TeacherSubjectPort {

	/** Matières encore affichées. Une ligne cachée n'est pas dans cette liste. */
	List<StoredSubject> findByUser(long userId);

	StoredSubject insert(long userId, long schoolId, String grade, String name, String nameKey);

	Optional<StoredSubject> findOwned(long userId, long subjectId);

	Optional<StoredSubject> rename(long userId, long subjectId, String name, String nameKey);

	/**
	 * La matière reste dans la liste du professeur. Le reste de l'application ne la propose plus.
	 *
	 * @return faux si elle n'est pas affichée pour ce professeur
	 */
	boolean deactivate(long userId, long subjectId);

	/**
	 * @return faux si elle n'est pas affichée pour ce professeur
	 */
	boolean reactivate(long userId, long subjectId);

	/**
	 * Retire la matière de l'affichage. La ligne reste en base.
	 *
	 * @return faux si elle n'appartient pas à ce professeur, ou si elle est déjà cachée
	 */
	boolean hide(long userId, long subjectId);

	/**
	 * Vrai si une matière encore affichée de cette école et de cette année a déjà ce nom.
	 * Le professeur qui l'a créée n'importe pas : la classe n'en a qu'une.
	 *
	 * @param exceptId matière en cours de renommage, ignorée ; null à la création
	 */
	boolean nameTaken(long schoolId, String grade, String nameKey, Long exceptId);

	/**
	 * @param grade  année scolaire, le libellé du profil (CE1, 3e primaire)
	 * @param active faux : visible seulement chez le professeur
	 */
	record StoredSubject(long id, long userId, long schoolId, String grade, String name, boolean active) {
	}

	/**
	 * Une classe du profil : une école et une année. Sert à savoir quelles matières garder.
	 */
	record SubjectClassKey(long schoolId, String grade) {
	}
}
