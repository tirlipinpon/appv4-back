package com.appv4.auth.application.result;

/**
 * Jeton JWT émis pour l'API admin.
 */
public record JwtTokenResult(String token, long expiresAtEpochSeconds) {
}
