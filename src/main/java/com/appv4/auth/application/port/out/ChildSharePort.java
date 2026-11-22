package com.appv4.auth.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Détenteurs d'une fiche et invitations (BACK-74).
 *
 * <p><b>Business :</b> plusieurs parents peuvent avoir la même fiche. Le parent
 * référent invite. Tant que l'email n'accepte pas, il ne l'a pas.
 * <p><b>Technique :</b> tables {@code back_child_holders} et {@code back_child_invitations}.
 * <p><b>Métier / flow :</b> le cas d'usage des fiches décide. Ce port garde les lignes.
 */
public interface ChildSharePort {

	/**
	 * @return vrai si ce parent a la fiche encore affichée, y compris parce qu'il l'a inscrite
	 */
	boolean holds(long userId, long childId);

	/**
	 * @return vrai si ce parent gère la fiche encore affichée
	 */
	boolean isReferent(long userId, long childId);

	boolean pending(long childId, String email);

	StoredInvitation invite(long childId, String email);

	List<StoredInvitation> pendingForEmail(String email);

	Optional<StoredInvitation> findPending(long invitationId);

	void accept(long invitationId, long userId);

	void cancelPending(long childId);

	/**
	 * @param email déjà coupé et en minuscules
	 */
	record StoredInvitation(long id, long childId, String email) {
	}
}
