package com.appv4.auth.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Réponse JWT commune (login email, signup, OAuth).
 *
 * <p><b>Business :</b> le front stocke le token pour appeler {@code /api/int/**}.
 * <p><b>Technique :</b> DTO HTTP partagé — un seul schéma OpenAPI {@code JwtTokenResponse}.
 */
@Schema(name = "JwtTokenResponse")
public record JwtTokenResponse(String token, long expiresAtEpochSeconds) {
}
