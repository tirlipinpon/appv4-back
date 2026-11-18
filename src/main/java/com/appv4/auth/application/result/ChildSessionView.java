package com.appv4.auth.application.result;

/**
 * Enfant encore jouable derrière un jeton (BACK-13).
 *
 * <p><b>Business :</b> le prénom affiché vient de la fiche, pas d'un texte saisi au login.
 */
public record ChildSessionView(long childId, String firstName) {
}
