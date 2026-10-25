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
 * profil retire une année, le service profil appelle {@link #delete} pour les matières
 * de cette année.
 */
public interface TeacherSubjectPort {

	List<StoredSubject> findByUser(long userId);

	StoredSubject insert(long userId, long schoolId, String grade, String name, String nameKey);

	Optional<StoredSubject> findOwned(long userId, long subjectId);

	Optional<StoredSubject> rename(long userId, long subjectId, String name, String nameKey);

	/**
	 * @return faux si la matière n'appartient pas à ce professeur
	 */
	boolean delete(long userId, long subjectId);

	/**
	 * Vrai si une autre matière de cette classe a déjà ce nom, à la casse près.
	 *
	 * @param exceptId matière en cours de renommage, ignorée ; null à la création
	 */
	boolean nameTaken(long userId, long schoolId, String grade, String nameKey, Long exceptId);

	/**
	 * @param grade année scolaire, le libellé du profil (CE1, 3e primaire)
	 */
	record StoredSubject(long id, long userId, long schoolId, String grade, String name) {
	}

	/**
	 * Une classe du profil : une école et une année. Sert à savoir quelles matières garder.
	 */
	record SubjectClassKey(long schoolId, String grade) {
	}
}
