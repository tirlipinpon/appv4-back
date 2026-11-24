package com.appv4.ai.application;

/**
 * Une connexion lisible par l'admin (BACK-84).
 *
 * <p><b>Business :</b> le fournisseur et le modèle se relisent. La clé ne revient pas.
 */
public record ConnexionModele(long id, String fournisseur, String modele) {
}
