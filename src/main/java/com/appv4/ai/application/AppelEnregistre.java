package com.appv4.ai.application;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Un appel déjà fait pour un besoin (BACK-31).
 *
 * <p><b>Business :</b> on garde le modèle qui a répondu, pas le texte envoyé.
 * <p><b>Technique :</b> pas de prompt ni de réponse. {@code usageSignale} est faux
 * quand le fournisseur n'a pas donné de tokens.
 */
public record AppelEnregistre(
		String codeBesoin,
		String fournisseur,
		String modele,
		LocalDate jourUtc,
		int tokensPrompt,
		int tokensReponse,
		int tokensTotal,
		boolean usageSignale,
		String statut,
		long dureeMillis,
		Instant instant) {
}
