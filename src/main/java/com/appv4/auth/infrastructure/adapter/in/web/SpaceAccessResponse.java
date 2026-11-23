package com.appv4.auth.infrastructure.adapter.in.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * État lu par l'en-tête (SCRUM-152). Le JWT ne porte pas la demande professeur
 * quand la personne a déjà un espace.
 *
 * <p><b>Business :</b> savoir s'il faut « Demander », « Voir ma demande » ou « Ouvrir ».
 * <p><b>Technique :</b> {@code roles} = espaces accordés. {@code teacherRequest} =
 * {@code none}, {@code pending}, {@code approved} ou {@code revoked} (retrait admin).
 */
@Schema(name = "SpaceAccessResponse")
public record SpaceAccessResponse(
		@Schema(description = "Rôles d'espace déjà accordés : parent et/ou teacher")
		List<String> roles,
		@Schema(
				description = "Demande professeur : none, pending, approved ou revoked",
				allowableValues = { "none", "pending", "approved", "revoked" })
		String teacherRequest) {
}
