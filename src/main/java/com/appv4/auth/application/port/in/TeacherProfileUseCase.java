package com.appv4.auth.application.port.in;

import java.util.List;

import com.appv4.auth.application.result.TeacherProfileView;
import com.appv4.auth.application.result.TeacherRemovalPreviewView;

/**
 * Lire et enregistrer le profil du professeur connecté (SCRUM-159).
 *
 * <p><b>Business :</b> il corrige son nom, et rattache des écoles seulement
 * si le référentiel en propose. Le mot de passe et les rôles ne sont pas ici.
 * <p><b>Technique :</b> l'identifiant vient du jeton, jamais du corps de la requête.
 * <p><b>Métier / flow :</b> port in de {@code /api/int/teacher/profile},
 * après le filtre JWT, avant les ports nom et référentiel d'écoles.
 */
public interface TeacherProfileUseCase {

	TeacherProfileView load(long userId, String activeRole);

	TeacherProfileView update(long userId, String activeRole, String displayName, List<TeacherSchoolCommand> schools);

	/**
	 * Élèves et matières concernés avant de retirer une année ou une école.
	 *
	 * @param grade année à retirer, ou null pour l'école entière
	 */
	TeacherRemovalPreviewView removalPreview(long userId, String activeRole, long schoolId, String grade);

	/**
	 * @param schoolId   null si le bloc n'a pas encore d'école choisie
	 * @param curriculum code de norme, ou null
	 * @param grades     années cochées, éventuellement vides
	 */
	record TeacherSchoolCommand(Long schoolId, String curriculum, List<String> grades) {
	}
}
