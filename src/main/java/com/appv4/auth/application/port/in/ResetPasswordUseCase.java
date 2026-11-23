package com.appv4.auth.application.port.in;

/**
 * Port entrant — renew mot de passe via jeton (SCRUM-111).
 *
 * <p><b>Business :</b> l'utilisateur pose un nouveau mdp avec le lien reçu par email.
 * <p><b>Technique :</b> valide token hashé + règles mdp, puis UPDATE password_hash.
 * <p><b>Métier / flow :</b> controller → ce port → ports out token + user account.
 */
public interface ResetPasswordUseCase {

	/**
	 * Consomme le jeton clair et pose le nouveau mot de passe.
	 *
	 * @throws com.appv4.auth.application.exception.AuthDomainException si jeton invalide / mdp faible
	 */
	void reset(String clearToken, String newPassword);
}
