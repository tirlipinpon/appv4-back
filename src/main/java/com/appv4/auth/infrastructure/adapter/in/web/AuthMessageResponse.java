package com.appv4.auth.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Message générique auth (forgot / reset password).
 *
 * <p><b>Technique :</b> DTO HTTP partagé — un seul schéma OpenAPI {@code AuthMessageResponse}.
 */
@Schema(name = "AuthMessageResponse")
public record AuthMessageResponse(String message) {
}
