package com.appv4.auth.application.port.out;

import java.util.Optional;

/**
 * Téléphone, mail de contact, et marque « question déjà posée ».
 *
 * <p><b>Business :</b> ces deux infos servent à retrouver l'email de connexion.
 * La fiche parent ou professeur les corrige sans poser la marque.
 * <p><b>Technique :</b> colonnes du compte. La marque ne revient pas à faux.
 * <p><b>Métier / flow :</b> port out de la sollicitation et de la lecture du bandeau.
 */
public interface RecoveryPromptPort {

	Optional<RecoveryPromptState> find(long userId);

	/** Pose la marque. Ne change ni le nom, ni le téléphone, ni le mail de contact. */
	void markAsked(long userId);

	/** Enregistre le nom et le contact, puis pose la marque. */
	void saveAndAsk(long userId, String displayName, String phone, String contactEmail);

	/**
	 * @param asked vrai si la sollicitation a déjà été passée ou enregistrée
	 */
	record RecoveryPromptState(String displayName, String phone, String contactEmail, boolean asked) {
	}
}
