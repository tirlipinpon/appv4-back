package com.appv4.ai.application;

import java.time.LocalDate;

/**
 * Le cumul d'un jour universel pour un besoin et un modèle (BACK-31).
 *
 * <p><b>Business :</b> deux modèles du même besoin ne s'additionnent pas.
 * <p><b>Technique :</b> le jour est UTC, pas la journée de l'enfant.
 */
public record CumulJour(
		LocalDate jourUtc,
		String codeBesoin,
		String fournisseur,
		String modele,
		int appels,
		int tokens) {
}
