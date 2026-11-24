package com.appv4.ai.application;

/**
 * Un changement de paire, le plus récent d'abord à la lecture (BACK-32).
 *
 * <p><b>Business :</b> le nom de l'admin et l'heure de Bruxelles viennent de l'enregistrement, pas de la page.
 */
public record ChangementBesoin(
		String code,
		String nom,
		String nomAdmin,
		String dateLabel,
		String timeLabel,
		String ancienFournisseur,
		String ancienModele,
		String nouveauFournisseur,
		String nouveauModele) {
}
