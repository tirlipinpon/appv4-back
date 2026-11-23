package com.appv4.auth.application.port.in;

import java.util.List;

import com.appv4.auth.application.port.out.HumeurAnswerPort.StoredAnswer;
import com.appv4.auth.application.port.out.HumeurChoicePort.HumeurChoice;

/**
 * Demander l'humeur de l'enfant et garder sa réponse (BACK-69).
 *
 * <p><b>Business :</b> on demande selon la tendance, et on garde le mot et l'icône vus.
 * <p><b>Technique :</b> jeton enfant. La liste est vide quand on ne demande pas.
 * <p><b>Métier / flow :</b> accueil → ce port → essais déjà enregistrés et bibliothèque.
 */
public interface AskChildHumeurUseCase {

	Prompt ask(String rawToken);

	StoredAnswer answer(String rawToken, Long choiceId, Boolean closed);

	/**
	 * @param message phrase à montrer, absente quand on ne demande pas
	 * @param choices vide quand {@code ask} est faux
	 */
	record Prompt(boolean ask, String message, List<HumeurChoice> choices) {
	}
}
