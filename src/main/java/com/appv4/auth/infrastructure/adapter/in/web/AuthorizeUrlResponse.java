package com.appv4.auth.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * URL d'autorisation OAuth (Google / Facebook / Apple) + state navigateur (issue #32).
 *
 * <p><b>Technique :</b> DTO HTTP partagé — un seul schéma OpenAPI {@code AuthorizeUrlResponse}.
 * Le FE stocke {@code state} (sessionStorage) et le renvoie à l'échange de code.
 */
@Schema(name = "AuthorizeUrlResponse")
public record AuthorizeUrlResponse(String authorizationUrl, String state) {
}
