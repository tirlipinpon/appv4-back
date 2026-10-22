package com.appv4.auth.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * URL d'autorisation OAuth (Google / Facebook / Apple).
 *
 * <p><b>Technique :</b> DTO HTTP partagé — un seul schéma OpenAPI {@code AuthorizeUrlResponse}.
 */
@Schema(name = "AuthorizeUrlResponse")
public record AuthorizeUrlResponse(String authorizationUrl) {
}
