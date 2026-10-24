package com.appv4.auth.application.port.in;

import com.appv4.auth.application.result.ParentChildrenView;
import com.appv4.auth.application.port.out.ChildPort.ChildPhoto;

/**
 * Ajouter et relire les enfants du parent connecté (SCRUM-156).
 *
 * <p><b>Business :</b> le parent ajoute, relit, corrige, désactive ou retire ses enfants de l'affichage.
 * Désactivé : encore dans la liste, plus proposé ailleurs. Retiré : plus affiché, fiche conservée.
 * Un PIN vide à la modification garde l'ancien code. L'enfant ne se connecte pas encore avec.
 * <p><b>Technique :</b> l'identifiant vient du jeton. Le PIN clair ne sort pas du service.
 * <p><b>Métier / flow :</b> port in de {@code /api/int/parent/children}, espace parent seulement.
 */
public interface ParentChildrenUseCase {

	ParentChildrenView list(long parentUserId, String activeRole);

	ParentChildrenView.ChildCard add(long parentUserId, String activeRole, NewChildCommand command);

	ParentChildrenView.ChildCard update(long parentUserId, String activeRole, long childId, NewChildCommand command);

	void deactivate(long parentUserId, String activeRole, long childId);

	void reactivate(long parentUserId, String activeRole, long childId);

	void hide(long parentUserId, String activeRole, long childId);

	ChildPhoto photo(long parentUserId, String activeRole, long childId);

	/**
	 * @param age               texte saisi, parsé ensuite (2 à 18)
	 * @param schoolId          école de l'annuaire, ou null
	 * @param pinConfirmation   seconde saisie, comparée puis oubliée
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
			String pinConfirmation) {
	}
}
