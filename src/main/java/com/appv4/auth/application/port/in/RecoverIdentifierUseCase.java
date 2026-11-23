package com.appv4.auth.application.port.in;

/**
 * Récupération de l'identifiant : montrer l'email de connexion masqué, ou une phrase.
 *
 * <p><b>Business :</b> sans preuve, on ne change ni l'email ni le mot de passe.
 * <p><b>Technique :</b> un masque ou un message, jamais les deux, jamais l'adresse entière.
 * <p><b>Métier / flow :</b> port in appelé par la route publique, après la préparation des trois infos.
 */
public interface RecoverIdentifierUseCase {

	IdentifierRecovery recover(String displayName, String phone, String contactEmail);

	/**
	 * @param maskedEmail masque de l'email de connexion, absent quand il y a un message
	 * @param message     phrase à afficher, absente quand il y a un masque
	 */
	record IdentifierRecovery(String maskedEmail, String message) {
	}
}
