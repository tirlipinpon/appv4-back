package com.appv4.ai.application.port.out;

import java.util.List;

import com.appv4.ai.application.CompletionIa;
import com.appv4.ai.application.MessageIa;

/**
 * Un fournisseur qui sait répondre pour un modèle (BACK-30).
 *
 * <p><b>Business :</b> le métier ne choisit pas la maison. Il nomme un besoin.
 * <p><b>Technique :</b> {@code code} est le nom de l'adaptateur, par exemple {@code openai}.
 * <p><b>Métier / flow :</b> le routeur du besoin appelle cet port après avoir lu la route.
 */
public interface FournisseurIaPort {

	String code();

	CompletionIa completer(String modele, List<MessageIa> messages);

	/**
	 * Essai d'une connexion dont la clé est celle envoyée, pas celle de la config.
	 */
	default CompletionIa completer(String modele, List<MessageIa> messages, String cle) {
		return completer(modele, messages);
	}
}
