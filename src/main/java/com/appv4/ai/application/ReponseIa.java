package com.appv4.ai.application;

/**
 * Texte renvoyé pour un besoin, avec le modèle qui a répondu (BACK-30).
 *
 * <p><b>Business :</b> le fournisseur et le modèle sont ceux de la route au moment de l'appel.
 * <p><b>Technique :</b> l'usage peut être absent. Il n'est pas encore cumulé.
 */
public record ReponseIa(String fournisseur, String modele, String texte, UsageIa usage) {
}
