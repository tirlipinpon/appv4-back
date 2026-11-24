package com.appv4.ai.application.port.out;

import java.time.Instant;

/**
 * Ligne brute d'historique, avant les libellés de Bruxelles (BACK-32).
 *
 * <p><b>Business :</b> l'ancienne paire reste. Un refus n'écrit pas cette ligne.
 */
public record ChangementEnregistre(
		String code,
		String nom,
		String nomAdmin,
		Instant instant,
		String ancienFournisseur,
		String ancienModele,
		String nouveauFournisseur,
		String nouveauModele) {
}
