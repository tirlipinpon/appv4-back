package com.appv4.auth.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Nom lu par le bandeau des pages connectées.
 *
 * <p><b>Business :</b> c'est le nom du parent ou du professeur, pas le rôle.
 * <p><b>Technique :</b> un seul champ, toujours renseigné quand le compte existe.
 */
@Schema(name = "ConnectedAccountResponse")
public record ConnectedAccountResponse(
		@Schema(description = "Nom du compte connecté (parent, professeur ou admin)")
		String displayName) {
}
