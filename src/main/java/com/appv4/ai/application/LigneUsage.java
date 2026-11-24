package com.appv4.ai.application;

import java.time.LocalDate;

/**
 * Une ligne de consommation déjà comptée (BACK-32).
 *
 * <p><b>Business :</b> le jour est UTC. Les appels et les tokens sont ceux du cumul, pas une somme de la page.
 */
public record LigneUsage(
		LocalDate jour,
		String code,
		String nom,
		String fournisseur,
		String modele,
		int appels,
		int tokens) {
}
