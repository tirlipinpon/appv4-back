package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Nom du compte et écoles du professeur connecté (SCRUM-159).
 *
 * <p><b>Business :</b> le nom affiché est celui du compte, le même pour l'espace parent.
 * Les écoles et les années ne concernent que ce professeur.
 * <p><b>Technique :</b> {@code back_users.display_name} uniquement pour le nom.
 * Email, mot de passe, rôles et version de sécurité ne bougent pas.
 * Les rattachements sont dans {@code back_teacher_schools} et {@code back_teacher_school_grades}.
 * <p><b>Métier / flow :</b> port out de {@code GET/PUT /api/int/teacher/profile}.
 */
public interface TeacherProfilePort {

	Optional<TeacherAccount> findAccount(long userId);

	/**
	 * Remplace le nom affiché. Ne déconnecte pas : {@code security_version} reste.
	 */
	void updateDisplayName(long userId, String displayName);

	List<TeacherSchoolLink> findSchools(long userId);

	/**
	 * Remplace toute la liste. Une liste vide retire les rattachements de ce professeur.
	 */
	void replaceSchools(long userId, List<TeacherSchoolLink> schools);

	/**
	 * Comptes qui ont cette école et cette année sur leur profil (SCRUM-189).
	 * Le filtre « rôle teacher accordé » se fait ensuite dans le service.
	 */
	List<Long> findUserIdsWithClass(long schoolId, String grade);

	/**
	 * @param email       email du compte, jamais modifié par ce port
	 * @param displayName nom affiché actuel
	 */
	record TeacherAccount(String email, String displayName) {
	}

	/**
	 * @param schoolId   école du référentiel
	 * @param curriculum {@code FRANCE} ou {@code BELGIUM_FR}
	 * @param grades     libellés d'années de cette norme, ordre officiel
	 */
	record TeacherSchoolLink(long schoolId, String curriculum, List<String> grades) {
		public TeacherSchoolLink {
			grades = grades == null ? List.of() : List.copyOf(grades);
		}
	}
}
