package com.appv4.auth.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.appv4.auth.application.exception.AuthDomainException;
import com.appv4.auth.application.exception.AuthErrorCode;
import com.appv4.auth.application.port.in.RecoveryPromptUseCase;
import com.appv4.auth.application.port.out.RecoveryPromptPort;
import com.appv4.auth.application.port.out.RecoveryPromptPort.RecoveryPromptState;

/**
 * Sollicitation du téléphone et du mail de contact.
 *
 * <p><b>Business :</b> on demande tant que la personne ne l'a pas vue et qu'une des deux infos manque.
 * Passer n'écrit pas ces infos. Un enregistrement refusé ne compte pas comme vu.
 * <p><b>Technique :</b> les règles de contact sont celles de la fiche. La marque ne se remet pas à faux.
 * <p><b>Métier / flow :</b> port in → port out. Le bandeau relit le même résultat.
 */
public class RecoveryPromptService implements RecoveryPromptUseCase {

	public static final String PROMPT =
			"Ces informations serviront à retrouver ton email de connexion si tu le perds.";

	public static final String ACCOUNT_MISSING = "Compte introuvable.";

	private final RecoveryPromptPort prompts;

	public RecoveryPromptService(RecoveryPromptPort prompts) {
		this.prompts = prompts;
	}

	@Override
	@Transactional(readOnly = true)
	public RecoveryPrompt read(long userId) {
		return toView(require(userId));
	}

	@Override
	@Transactional
	public void answer(long userId, boolean skip, String displayName, String phone, String contactEmail) {
		require(userId);
		if (skip) {
			prompts.markAsked(userId);
			return;
		}
		String name = AccountContact.name(displayName);
		String nextPhone = AccountContact.phone(phone);
		String nextEmail = AccountContact.email(contactEmail);
		prompts.saveAndAsk(userId, name, nextPhone, nextEmail);
	}

	private RecoveryPromptState require(long userId) {
		return prompts.find(userId)
				.orElseThrow(() -> new AuthDomainException(AuthErrorCode.NOT_FOUND, ACCOUNT_MISSING));
	}

	private static RecoveryPrompt toView(RecoveryPromptState state) {
		boolean missing = blank(state.phone()) || blank(state.contactEmail());
		boolean ask = !state.asked() && missing;
		return new RecoveryPrompt(ask, ask ? PROMPT : null);
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}
}
