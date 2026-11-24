package com.appv4.ai.application;

/**
 * Compteurs renvoyés par le fournisseur (BACK-30).
 *
 * <p><b>Business :</b> ce sont des tokens, pas le jeton de connexion.
 * <p><b>Technique :</b> vide quand le fournisseur n'en envoie pas. Ce ticket ne les enregistre pas.
 */
public record UsageIa(Integer tokensPrompt, Integer tokensReponse) {
}
