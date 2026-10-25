package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.ParentChildrenView;
import com.appv4.auth.application.port.out.ChildPort.ChildPhoto;

/**
 * Ajouter et relire les enfants du parent connecté (SCRUM-156).
 *
 * <p><b>Business :</b> le parent déclare qui utilisera l'app, avec une classe et un code.
 * Il ne modifie pas encore une fiche, et l'enfant ne se connecte pas avec ce code.
 * <p><b>Technique :</b> l'identifiant vient du jeton. Le PIN clair ne sort pas du service.
 * <p><b>Métier / flow :</b> port in de {@code /api/int/parent/children}, espace parent seulement.
 */
public interface ParentChildrenUseCase {

	ParentChildrenView list(long parentUserId, String activeRole);

	ParentChildrenView.ChildCard add(long parentUserId, String activeRole, NewChildCommand command);

	ChildPhoto photo(long parentUserId, String activeRole, long childId);

	/**
	 * @param age               texte saisi, parsé ensuite (2 à 18)
	 * @param schoolId          école de l'annuaire, ou null
	 * @param pinConfirmation   seconde saisie, comparée puis oubliée
	 * @param fromChildId       enfant de départ, ou null pour un formulaire vide
	 * @param link              code fermé, ou {@code AUCUN} pour ne pas enregistrer de lien
	 */
	record NewChildCommand(
			String firstName,
			String lastName,
			String age,
			String country,
			String grade,
			Long schoolId,
			String avatarCode,
			String photoBase64,
			String photoContentType,
			String pin,
			String pinConfirmation,
			Long fromChildId,
			String link) {
	}
}
