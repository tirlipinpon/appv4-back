package com.appv4.ai.application.port.out;

/**
 * La route d'un besoin vers un fournisseur et un modèle (BACK-30).
 *
 * <p><b>Business :</b> un besoin inactif ne part pas. Le nom est celui que l'admin lira.
 * <p><b>Technique :</b> {@code code} est stable ({@code question-draft}, {@code answer-analysis},
 * {@code learner-support}).
 */
public record RouteBesoin(String code, String nom, String fournisseur, String modele, boolean actif) {
}
