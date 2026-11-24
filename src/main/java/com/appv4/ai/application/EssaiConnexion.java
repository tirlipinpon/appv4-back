package com.appv4.ai.application;

/**
 * Résultat d'un essai de connexion (BACK-84).
 *
 * <p><b>Business :</b> succès, ou le message du fournisseur. Pas une réponse inventée.
 * <p><b>Technique :</b> ce n'est pas une consommation de besoin.
 */
public record EssaiConnexion(boolean succes, String message) {
}
