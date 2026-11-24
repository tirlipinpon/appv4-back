package com.appv4.ai.application;

/**
 * Un tour envoyé au modèle (BACK-30).
 *
 * <p><b>Business :</b> le rôle dit qui parle, le texte est ce qu'il a dit.
 * <p><b>Technique :</b> ce n'est pas une page de question. Rien ici n'est encore compté en tokens.
 */
public record MessageIa(String role, String texte) {
}
