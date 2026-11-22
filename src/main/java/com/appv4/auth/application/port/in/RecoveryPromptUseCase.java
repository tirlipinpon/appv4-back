package com.appv4.auth.application.port.in;

/**
 * Décide si l'adulte doit encore entendre à quoi servent le téléphone et le mail de contact.
 *
 * <p><b>Business :</b> une seule fois, et seulement s'il manque l'un des deux.
 * Passer ou enregistrer suffit. La fiche peut les remplir plus tard sans reposer la question.
 * <p><b>Technique :</b> la phrase est fixe. Le front l'affiche, il ne l'écrit pas.
 * <p><b>Métier / flow :</b> port in lu par GET /api/int/auth/me, écrit par POST /api/int/auth/recovery-prompt.
 */
public interface RecoveryPromptUseCase {

	RecoveryPrompt read(long userId);

	void answer(long userId, boolean skip, String displayName, String phone, String contactEmail);

	/**
	 * @param recoveryPrompt phrase à montrer, absente quand il ne faut pas demander
	 */
	record RecoveryPrompt(boolean ask, String recoveryPrompt) {
	}
}
