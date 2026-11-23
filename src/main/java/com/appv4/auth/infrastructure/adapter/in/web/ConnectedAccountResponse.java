package com.appv4.auth.infrastructure.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;

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
		String displayName,
		@Schema(nullable = true, description = "Code d'avatar, absent s'il n'y en a pas")
		String avatarCode,
		@Schema(description = "Vrai s'il faut encore demander le téléphone et le mail de contact")
		boolean askRecoveryContact,
		@JsonInclude(JsonInclude.Include.NON_NULL)
		@Schema(nullable = true, description = "Phrase de la sollicitation, absente quand il ne faut pas demander")
		String recoveryPrompt) {
}
