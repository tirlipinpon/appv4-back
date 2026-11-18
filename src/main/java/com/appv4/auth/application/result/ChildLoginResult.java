package com.appv4.auth.application.result;

/**
 * Session ouverte pour un enfant (BACK-13).
 *
 * @param token                 JWT enfant, à renvoyer en Bearer sur l'app learning
 * @param expiresAtEpochSeconds fin de validité, epoch secondes
 * @param firstName             prénom tel qu'il est écrit sur la fiche, pour l'accueil
 */
public record ChildLoginResult(String token, long expiresAtEpochSeconds, String firstName) {
}
