package com.appv4.auth.application.port.in;

import java.util.List;

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
	 * Invite un email sur une fiche que ce parent gère.
	 *
	 * <p><b>Business :</b> la réponse ne dit pas si le compte existe. Rien n'est partagé avant l'acceptation.
	 */
	InvitationReceipt invite(long parentUserId, String activeRole, long childId, String email);

	/** Invitations encore ouvertes pour l'email du compte connecté. */
	List<PendingInvitation> pendingInvitations(long parentUserId, String activeRole);

	/** Le compte dont l'email est celui de l'invitation reçoit la fiche. */
	ParentChildrenView.ChildCard acceptInvitation(long parentUserId, String activeRole, long invitationId);

	/**
	 * Demande une fiche par l'email du parent référent, le prénom et le nom.
	 *
	 * <p><b>Business :</b> l'écran dit toujours que c'est envoyé, sauf si la fiche est déjà chez le demandeur.
	 */
	InvitationReceipt requestShare(long parentUserId, String activeRole, String email, String firstName, String lastName);

	List<ShareRequestView> shareRequests(long parentUserId, String activeRole);

	void acceptShareRequest(long parentUserId, String activeRole, long requestId, long childId);

	void refuseShareRequest(long parentUserId, String activeRole, long requestId);

	void cancelShareRequest(long parentUserId, String activeRole, long requestId);

	record InvitationReceipt(String message) {
	}

	record PendingInvitation(long id, long childId, String firstName, String lastName) {
	}

	record ShareRequestChild(long childId, String firstName, String lastName) {
	}

	record ShareRequestView(long id, String firstName, String lastName, List<ShareRequestChild> matches) {
		public ShareRequestView {
			matches = matches == null ? List.of() : List.copyOf(matches);
		}
	}

	/**
	 * @param age               texte saisi, parsé ensuite (2 à 18)
	 * @param schoolId          école de l'annuaire, ou null
	 * @param pinConfirmation   seconde saisie, comparée puis oubliée
	 * @param fromChildId       enfant de départ, ou null pour un formulaire vide
	 * @param link              code fermé, ou {@code AUCUN} pour ne pas enregistrer de lien
	 * @param interests         centres d'intérêt. null : liste absente. vide : effacer
	 */
	record NewChildCommand(
			String firstName,
			String lastName,
			String sex,
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
			String link,
			List<String> interests) {
	}
}
