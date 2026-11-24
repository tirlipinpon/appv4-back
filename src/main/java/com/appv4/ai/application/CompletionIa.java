package com.appv4.ai.application;

/**
 * Complétion brute d'un fournisseur (BACK-30).
 *
 * <p><b>Business :</b> le texte est la réponse du modèle, pas une phrase de secours.
 * <p><b>Technique :</b> l'usage est celui du fournisseur, ou vide.
 */
public record CompletionIa(String texte, UsageIa usage) {
}
