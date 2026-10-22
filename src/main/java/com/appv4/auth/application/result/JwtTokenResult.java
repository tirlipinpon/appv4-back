package com.appv4.auth.application.result;

/**
 * Réponse d'émission JWT destinée au front (login / oauth à venir).
 *
 * @param token                 JWT compact (3 parties base64, à mettre en Bearer)
 * @param expiresAtEpochSeconds date d'expiration (epoch secondes) pour le client
 */
public record JwtTokenResult(String token, long expiresAtEpochSeconds) {
}
